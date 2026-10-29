#!/usr/bin/env python3
"""Check optional integration startup in disposable dedicated servers."""
import argparse
import hashlib
import json
import shutil
import subprocess
import tempfile
import time
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server', type=Path, required=True, help='Read-only installed game directory')
    parser.add_argument('--java', required=True)
    parser.add_argument('--scenario', choices=['standalone', 'all-disabled', 'terrain-disabled', 'terrain', 'full',
                                             'unsupported-disabled', 'unsupported-enabled'], required=True)
    parser.add_argument('--mod', type=Path)
    args = parser.parse_args()
    server = args.server.expanduser().resolve()
    version = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines()
                   if line.startswith('mod_version='))
    mod = args.mod or ROOT / f'build/libs/create-sky-compat-{version}-mc1.21.1-neoforge.jar'
    launch = list((server / 'libraries/net/neoforged/neoforge').glob('*/unix_args.txt'))
    if len(launch) != 1:
        parser.error('Expected exactly one installed NeoForge launch configuration')
    output = ROOT / 'build/verification'
    output.mkdir(parents=True, exist_ok=True)
    report = {'scenario': args.scenario, 'artifact': str(mod), 'sha256': hashlib.sha256(mod.read_bytes()).hexdigest()}
    selected = {}
    with tempfile.TemporaryDirectory(prefix='create-sky-startup-') as directory:
        run = Path(directory)
        (run / 'libraries').symlink_to(server / 'libraries', target_is_directory=True)
        (run / 'mods').mkdir()
        (run / 'config').mkdir()
        shutil.copy2(server / 'eula.txt', run / 'eula.txt')
        shutil.copy2(mod, run / 'mods' / mod.name)
        if args.scenario.startswith('unsupported-'):
            # Our own metadata-only fixture advertises future targets without providing their classes.
            # Disabled adapters must neither reject their versions nor resolve their APIs.
            metadata = 'modLoader="lowcodefml"\nloaderVersion="[1,)"\nlicense="MIT"\n'
            for target in ['sky_archipelago', 'skyvillages', 'architectury', 'sable', 'aeronautics_delivery_quests',
                           'distanthorizons', 'bluemap', 'analogaudio', 'create', 'veil', 'iris', 'sodium', 'aeronautics']:
                metadata += f'[[mods]]\nmodId="{target}"\nversion="999.0.0"\ndisplayName="Fixture {target}"\n'
            with zipfile.ZipFile(run / 'mods/future-targets-fixture.jar', 'w') as fixture:
                fixture.writestr('META-INF/neoforge.mods.toml', metadata)
        elif args.scenario != 'standalone':
            for source in sorted((server / 'mods').glob('*.jar')):
                with zipfile.ZipFile(source) as jar:
                    meta = (tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())
                            if 'META-INF/neoforge.mods.toml' in jar.namelist() else {})
                ids = {entry['modId'] for entry in meta.get('mods', [])}
                if ids & {'create_sky_compat', 'veil_iris_fix', 'sky_village_neighbors'}:
                    continue
                if args.scenario in {'terrain', 'terrain-disabled'} and 'sky_archipelago' not in ids:
                    continue
                shutil.copy2(source, run / 'mods' / source.name)
                selected[source.name] = hashlib.sha256(source.read_bytes()).hexdigest()
        if args.scenario in {'all-disabled', 'terrain-disabled', 'unsupported-disabled'}:
            import re
            keys = re.findall(r'\("([a-z_]+\.[a-z_]+)"', (ROOT / 'src/main/java/design/inherent/createskycompat/Feature.java').read_text())
            (run / 'config/create_sky_compat-features.properties').write_text(''.join(f'{key}=false\n' for key in keys))
        # The existing terrain fixture exercises the custom generator. Startup boundaries use vanilla flat.
        (run / 'server.properties').write_text(
            'server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-name=probe-world\n'
            'level-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-tick-time=0\n')
        command = [args.java, '-Xmx4G', '-Dmixin.debug.export=true', '-Dmixin.debug.verbose=true',
                   '@' + str(launch[0]), 'nogui']
        report['command'] = command
        log_path = output / f'startup-{args.scenario}.log'
        started = time.monotonic()
        done = False
        with log_path.open('w') as log:
            process = subprocess.Popen(command, cwd=run, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
            try:
                deadline = time.monotonic() + 240
                while process.poll() is None and time.monotonic() < deadline:
                    if 'Done (' in log_path.read_text(errors='replace'):
                        done = True
                        process.communicate('stop\n', timeout=90)
                        break
                    time.sleep(0.5)
            finally:
                if process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
        text = log_path.read_text(errors='replace')
        # Exported transformed targets must not reference Compat when no adapter was selected.
        references = []
        for target in (run / '.mixin.out').rglob('*.class'):
            if b'design/inherent/createskycompat/' in target.read_bytes():
                references.append(str(target.relative_to(run / '.mixin.out')))
        report.update(dependencies=selected, startup=done, exit_code=process.returncode,
                      seconds=round(time.monotonic() - started, 2), transformed_targets=references,
                      active_lines=[line for line in text.splitlines() if 'Create Sky Compat active adaptations:' in line],
                      generated_config=sorted(p.name for p in (run / 'config').iterdir()))
        inactive = args.scenario in {'standalone', 'all-disabled', 'terrain-disabled', 'unsupported-disabled'}
        report['passed'] = (done and process.returncode == 0 and bool(report['active_lines'])
                            and (not inactive or (not references and any('adaptations: []' in line for line in report['active_lines']))))
        if args.scenario == 'unsupported-enabled':
            report['passed'] = not done and 'terrain.preset requires sky_archipelago 1.3.6; found 999.0.0' in text
        (output / f'startup-{args.scenario}.json').write_text(json.dumps(report, indent=2) + '\n')
        print(json.dumps({key: report[key] for key in ['scenario', 'sha256', 'startup', 'exit_code', 'seconds', 'active_lines', 'passed']}, indent=2), flush=True)
        if not report['passed']:
            raise SystemExit(f'Startup check failed; inspect {log_path}')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Run the native placement fixture using an installed server's libraries and mods."""
import argparse
import hashlib
import time
import json
import shutil
import subprocess
import tempfile
import zipfile
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--server', type=Path, required=True, help='Installed game directory; read-only input')
parser.add_argument('--java', default='java')
parser.add_argument('--heap', choices=['4G', '6G', '8G'], default='4G', help='Maximum Java heap; distributed island scans need 6G')
parser.add_argument('--samples', type=int, default=12, help='Layouts per seed')
parser.add_argument('--reopen', action='store_true', help='Reopen generated samples and require unchanged ore counts or village starts')
parser.add_argument('--minimal', action='store_true', help='Generate with only Sky Archipelago and Compat, plus the read-only probe')
mode = parser.add_mutually_exclusive_group()
mode.add_argument('--generation', choices=['current', 'adapted', 'vanilla'], help='Generate and count bounded interior chunk samples')
mode.add_argument('--biomes', action='store_true', help='Inspect cave biome footprints and generated vegetation')
mode.add_argument('--mechanics', action='store_true', help='Exercise installed balloon searches across the expanded height')
mode.add_argument('--islands', action='store_true', help='Sample generated resources across three anchor footprints')
mode.add_argument('--chunk-futures', action='store_true', help='Compare background FULL requests with retained chunk tickets')
mode.add_argument('--terrain', action='store_true', help='Bounded native density survey without generating island chunks')
parser.add_argument('--natural-profile', choices=['current', 'adapted'], default='current', help='Preset for the natural village survey')
mode.add_argument('--natural', action='store_true', help='Generate natural starts and complete two villages')
parser.add_argument('--generation-config', type=Path, help='Explicit candidate geometry profile; installed config remains read-only')
parser.add_argument('--seed', type=int, default=1)
parser.add_argument('--maximum-gap', type=int, default=512)
parser.add_argument('--disable-carver-adapter', action='store_true',
                    help='Diagnostic control: omit relative-carving mixins from the staged mod only')
args = parser.parse_args()
if args.minimal and args.generation != 'adapted':
    parser.error('--minimal requires --generation adapted')
if args.reopen and not (args.generation or args.natural):
    parser.error('--reopen requires --generation or --natural')
if args.natural_profile != 'current' and not args.natural:
    parser.error('--natural-profile requires --natural')
server = args.server.expanduser().resolve()
version = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
mod = ROOT / f'build/libs/create-sky-compat-{version}-mc1.21.1-neoforge.jar'
fixture = ROOT / 'build/probe/create-sky-compat-probe.jar'
output = ROOT / 'build/verification'
output.mkdir(parents=True, exist_ok=True)
name = f'islands-{args.seed}' if args.islands else f'mechanics-{args.seed}' if args.mechanics else f'biomes-{args.seed}' if args.biomes else f'generation-{args.generation}-{args.seed}' if args.generation else 'terrain' if args.terrain else f'natural-{args.natural_profile}-{args.seed}' if args.natural else 'placement'
if args.chunk_futures: name = f'chunk-futures-{args.seed}'
if args.disable_carver_adapter:
    name += '-native-carvers'
if args.minimal:
    name += '-minimal'
report = output / (name + '.json')
report.unlink(missing_ok=True)
with tempfile.TemporaryDirectory(prefix='create-sky-compat-') as directory:
    run = Path(directory)
    (run / 'libraries').symlink_to(server / 'libraries', target_is_directory=True)
    (run / 'mods').mkdir()
    for source in (server / 'mods').glob('*.jar'):
        if source.name.startswith(('create-sky-compat-', 'sky-village-neighbors-')):
            continue
        if args.minimal:
            with zipfile.ZipFile(source) as jar:
                if 'META-INF/neoforge.mods.toml' not in jar.namelist():
                    continue
                metadata = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())
                if not any(mod['modId'] == 'sky_archipelago' for mod in metadata.get('mods', [])):
                    continue
        (run / 'mods' / source.name).symlink_to(source)
    if args.minimal and len(list((run / 'mods').glob('*.jar'))) != 1:
        raise SystemExit('Minimal run requires exactly one installed Sky Archipelago JAR')
    shutil.copy2(mod, run / 'mods' / mod.name)
    if args.disable_carver_adapter:
        staged = run / 'mods' / mod.name
        control = staged.with_suffix('.control')
        with zipfile.ZipFile(staged) as source, zipfile.ZipFile(control, 'w') as target:
            for entry in source.infolist():
                data = source.read(entry.filename)
                if entry.filename == 'create_sky_compat.mixins.json':
                    mixins = json.loads(data)
                    mixins['mixins'] = [name for name in mixins['mixins'] if name not in {
                        'CarvingContextMixin', 'CarverHeightMixin', 'CarverLavaMixin'}]
                    data = (json.dumps(mixins, indent=2) + '\n').encode()
                target.writestr(entry, data)
        control.replace(staged)
    shutil.copy2(fixture, run / 'mods' / fixture.name)
    for folder in ['config', 'defaultconfigs']:
        if not args.minimal and (server / folder).exists():
            shutil.copytree(server / folder, run / folder)
    (run / 'config').mkdir(exist_ok=True)
    (run / 'config/create_sky_compat-common.toml').write_text(
        f'enabled = true\nminimumGap = 64\nmaximumGap = {args.maximum_gap}\nminimumIslandRadius = 96\n')
    if args.generation_config:
        shutil.copy2(args.generation_config.expanduser().resolve(), run / 'config/create_sky_compat-generation.json')
    # Reuse the owner's existing EULA acceptance; never create acceptance implicitly.
    shutil.copy2(server / 'eula.txt', run / 'eula.txt')
    preset = {'current': 'sky_archipelago:sky_islands', 'adapted': 'create_sky_compat:islands_v1', 'vanilla': 'minecraft:normal'}.get(args.generation, ('create_sky_compat:islands_v1' if args.natural_profile == 'adapted' else 'sky_archipelago:sky_islands') if args.natural else 'minecraft:flat')
    if args.biomes or args.mechanics or args.islands or args.chunk_futures: preset = 'create_sky_compat:islands_v1'
    (run / 'server.properties').write_text('\n'.join([
        'server-ip=127.0.0.1', 'server-port=0', 'online-mode=false',
        'level-name=probe-world', f'level-type={preset}', f'level-seed={args.seed}',
        f'generate-structures={str(args.natural or args.chunk_futures).lower()}', 'view-distance=2',
        'max-tick-time=0', '',
    ]))
    launch_args = next((server / 'libraries/net/neoforged/neoforge').glob('*/unix_args.txt'))
    command = [args.java, f'-Xmx{args.heap}',
               f'-Dneighbors.probe.generation={args.generation or ""}',
               f'-Dneighbors.probe.biomes={str(args.biomes).lower()}',
               f'-Dneighbors.probe.mechanics={str(args.mechanics).lower()}',
               f'-Dneighbors.probe.islands={str(args.islands).lower()}',
               f'-Dneighbors.probe.chunkFutures={str(args.chunk_futures).lower()}',
               f'-Dneighbors.probe.terrain={str(args.terrain).lower()}',
               f'-Dneighbors.probe.expectedMaximumGap={args.maximum_gap}',
               f'-Dneighbors.probe.natural={str(args.natural).lower()}',
               f'-Dneighbors.probe.samples={args.samples}', f'-Dneighbors.probe.output={report}',
               '@' + str(launch_args), 'nogui']
    began = time.monotonic()
    def launch(log_path):
        with log_path.open('w') as log:
            subprocess.run(command, cwd=run, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=600)
        # Minecraft can exit zero after a shutdown exception, after writing the probe report.
        text = log_path.read_text(errors='replace')
        failures = [marker for marker in ['java.lang.OutOfMemoryError', 'Uncaught exception in server thread',
                    'Exception stopping the server', 'Encountered an unexpected exception'] if marker in text]
        if failures:
            if report.exists():
                data = json.loads(report.read_text())
                data.update(complete=False, runtimeFailures=failures, maximumHeap=args.heap)
                report.write_text(json.dumps(data, indent=2) + '\n')
            raise SystemExit(f'Native runtime failed: {", ".join(failures)}; inspect {log_path}')
    launch(output / (name + '.log'))
    if args.reopen and report.exists():
        first = json.loads(report.read_text())
        if first.get('complete') is False:
            raise SystemExit(f'Native check failed before reopening; diagnostics: {report}')
        report.unlink()
        launch(output / (name + '-reopen.log'))
        if report.exists():
            second = json.loads(report.read_text())
            def observed(data):
                if args.natural:
                    return {key: data[key] for key in ['samples', 'villagePlan']} if 'villagePlan' in data else data['samples']
                return [(sample['x'], sample['z'], {key: value for key, value in sample['blocks'].items()
                        if key.endswith('_ore')}) for sample in data['samples']]
            match_key = 'reopenVillagesMatch' if args.natural else 'reopenOreCountsMatch'
            if observed(first) != observed(second):
                second[match_key] = False
                second['firstObservations'] = observed(first)
                report.write_text(json.dumps(second, indent=2) + '\n')
                raise SystemExit('Observations changed after reopening; inspect generation reports and logs')
            second[match_key] = True
            report.write_text(json.dumps(second, indent=2) + '\n')
    if report.exists():
        data = json.loads(report.read_text())
        data['wallSecondsIncludingStartup'] = time.monotonic() - began
        data['maximumHeap'] = args.heap
        data['modSelection'] = 'minimal' if args.minimal else 'full'
        data['installedJars'] = sorted(path.name for path in (run / 'mods').glob('*.jar'))
        data['carverAdapterDisabled'] = args.disable_carver_adapter
        data['artifacts'] = {path.name: hashlib.sha256((run / 'mods' / path.name).read_bytes()).hexdigest() for path in [mod, fixture]}
        report.write_text(json.dumps(data, indent=2) + '\n')

if not report.exists():
    raise SystemExit(f'Probe failed; inspect {output / (name + ".log")}')
data = json.loads(report.read_text())
if data.get('complete') is False:
    raise SystemExit(f'Native check failed; diagnostics: {report}')
if args.chunk_futures:
    print('Chunk future comparison:', report)
    raise SystemExit(0)
if args.mechanics:
    print('Native mechanics survey:', report)
    raise SystemExit(0)
if args.islands:
    print('Island resource survey:', report)
    raise SystemExit(0)
if args.biomes:
    print('Cave biome survey:', report)
    raise SystemExit(0)
if args.generation:
    print('Generated block survey:', report, 'seconds:', data['elapsedSeconds'])
    raise SystemExit(0)
if args.terrain:
    print('Native terrain survey:', report, 'seconds:', data['elapsedSeconds'])
    raise SystemExit(0)
if args.natural:
    print('Natural starts:', data['accepted'], 'Complete villages:', data['completed'])
    raise SystemExit(0)
print('Native accepted:', data['nativeAccepted'], 'Mixin rejections:', data['mixinRejections'])
for distance in [384, 512, 768]:
    rows = [v for row in data['samples'] for v in row['variants'] if v['maximumGap'] == distance]
    times = sorted(row['ms'] for row in rows)
    print(distance, {kind: sum(row['decision'] == kind for row in rows) for kind in ['ACCEPTED', 'TOO_CLOSE', 'NO_HOST']},
          'median ms', times[len(times)//2], 'max ms', max(times))

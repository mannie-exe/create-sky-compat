#!/usr/bin/env python3
"""Run the native placement fixture using an installed server's libraries and mods."""
import argparse
import json
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--server', type=Path, required=True, help='Installed game directory; read-only input')
parser.add_argument('--java', default='java')
parser.add_argument('--samples', type=int, default=12, help='Layouts per seed')
parser.add_argument('--natural', action='store_true', help='Generate natural starts and complete two villages')
parser.add_argument('--seed', type=int, default=1)
parser.add_argument('--maximum-gap', type=int, default=512)
args = parser.parse_args()
server = args.server.expanduser().resolve()
version = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
mod = ROOT / f'build/libs/sky-village-neighbors-{version}-mc1.21.1-neoforge.jar'
fixture = ROOT / 'build/probe/sky-village-neighbors-probe.jar'
output = ROOT / 'build/verification'
output.mkdir(parents=True, exist_ok=True)
name = f'natural-{args.seed}' if args.natural else 'placement'
report = output / (name + '.json')
report.unlink(missing_ok=True)
with tempfile.TemporaryDirectory(prefix='sky-village-neighbors-') as directory:
    run = Path(directory)
    (run / 'libraries').symlink_to(server / 'libraries', target_is_directory=True)
    (run / 'mods').mkdir()
    for source in (server / 'mods').glob('*.jar'):
        if source.name.startswith('sky-village-neighbors-'):
            continue
        (run / 'mods' / source.name).symlink_to(source)
    shutil.copy2(mod, run / 'mods' / mod.name)
    shutil.copy2(fixture, run / 'mods' / fixture.name)
    for folder in ['config', 'defaultconfigs']:
        if (server / folder).exists():
            shutil.copytree(server / folder, run / folder)
    (run / 'config').mkdir(exist_ok=True)
    (run / 'config/sky_village_neighbors-common.toml').write_text(
        f'enabled = true\nminimumGap = 64\nmaximumGap = {args.maximum_gap}\nminimumIslandRadius = 96\n')
    # Reuse the owner's existing EULA acceptance; never create acceptance implicitly.
    shutil.copy2(server / 'eula.txt', run / 'eula.txt')
    preset = 'sky_archipelago:sky_islands' if args.natural else 'minecraft:flat'
    (run / 'server.properties').write_text('\n'.join([
        'server-ip=127.0.0.1', 'server-port=0', 'online-mode=false',
        'level-name=probe-world', f'level-type={preset}', f'level-seed={args.seed}',
        f'generate-structures={str(args.natural).lower()}', 'view-distance=2',
        'max-tick-time=0', '',
    ]))
    launch_args = next((server / 'libraries/net/neoforged/neoforge').glob('*/unix_args.txt'))
    with (output / (name + '.log')).open('w') as log:
        subprocess.run([args.java, '-Xmx4G',
                        f'-Dneighbors.probe.expectedMaximumGap={args.maximum_gap}', f'-Dneighbors.probe.natural={str(args.natural).lower()}', f'-Dneighbors.probe.samples={args.samples}', f'-Dneighbors.probe.output={report}',
                        '@' + str(launch_args), 'nogui'], cwd=run, stdout=log, stderr=subprocess.STDOUT,
                       check=True, timeout=600)
if not report.exists():
    raise SystemExit(f'Probe failed; inspect {output / (name + ".log")}')
data = json.loads(report.read_text())
if args.natural:
    print('Natural starts:', data['accepted'], 'Complete villages:', data['completed'])
    raise SystemExit(0)
print('Native accepted:', data['nativeAccepted'], 'Mixin rejections:', data['mixinRejections'])
for distance in [384, 512, 768]:
    rows = [v for row in data['samples'] for v in row['variants'] if v['maximumGap'] == distance]
    times = sorted(row['ms'] for row in rows)
    print(distance, {kind: sum(row['decision'] == kind for row in rows) for kind in ['ACCEPTED', 'TOO_CLOSE', 'NO_HOST']},
          'median ms', times[len(times)//2], 'max ms', max(times))

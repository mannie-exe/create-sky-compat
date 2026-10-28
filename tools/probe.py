#!/usr/bin/env python3
"""Run the native placement fixture using an installed server's libraries and mods."""
import resource
import sys
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
parser.add_argument('--mod', type=Path, help='Exact Compat JAR for an isolated comparison; defaults to the local build')
parser.add_argument('--server', type=Path, required=True, help='Installed game directory; read-only input')
parser.add_argument('--java', default='java')
parser.add_argument('--heap', choices=['4G', '6G', '8G'], default='4G', help='Maximum Java heap; distributed island scans need 6G')
parser.add_argument('--samples', type=int, default=12, help='Layouts per seed')
parser.add_argument('--reopen', action='store_true', help='Reopen generated samples and require unchanged ore counts or village starts')
parser.add_argument('--minimal', action='store_true', help='Generate with only Sky Archipelago and Compat, plus the read-only probe')
parser.add_argument('--dh-stop-after', type=int, default=0, metavar='SECONDS', help='With --dh-pregen, stop the disposable server during generation (1 to 30 seconds)')
parser.add_argument('--dh-world-key', action='store_true', help='With --dh-pregen, verify opt-in per-save cache identity in the disposable world')
mode = parser.add_mutually_exclusive_group()
mode.add_argument('--structures', action='store_true', help='Survey natural underground structure starts and realize critical layouts')
mode.add_argument('--surface-structures', action='store_true', help='Survey vanilla surface and water structure families')
mode.add_argument('--coverage', action='store_true', help='Export resolved worldgen resources and bounded native eligibility checks')
mode.add_argument('--benchmark', action='store_true', help='Measure a fixed column and generated-chunk workload')
mode.add_argument('--generation', choices=['current', 'adapted', 'vanilla'], help='Generate and count bounded interior chunk samples')
mode.add_argument('--surface-climate', action='store_true', help='Inspect contiguous surface biomes and native snow placement')
mode.add_argument('--biomes', action='store_true', help='Inspect cave biome footprints and generated vegetation')
mode.add_argument('--mechanics', action='store_true', help='Exercise installed balloon searches across the expanded height')
mode.add_argument('--islands', action='store_true', help='Sample generated resources across three anchor footprints')
mode.add_argument('--chunk-futures', action='store_true', help='Compare background FULL requests with retained chunk tickets')
mode.add_argument('--dh-pregen', action='store_true', help='Complete a bounded 32-chunk-radius DH job in a disposable full-pack server')
mode.add_argument('--terrain', action='store_true', help='Bounded native density survey without generating island chunks')
mode.add_argument('--overlap', action='store_true', help='Compare VOID and OVERLAP exterior columns at matched seeds')
mode.add_argument('--interiors', action='store_true', help='Sample cavity sections and compare independent direct/snapshot query order')
mode.add_argument('--veins', action='store_true', help='Find and verify native raw iron/copper vein material witnesses')
mode.add_argument('--water', action='store_true', help='Check local water material, height queries and native fluid ticks')
mode.add_argument('--materials', action='store_true', help='Inspect completed biome-specific island surfaces')
mode.add_argument('--oceans', action='store_true', help='Check experimental ocean habitats with an explicit --ocean-config')
parser.add_argument('--fluid-ticks', type=int, default=600, help='Native ticks for --water; 0 checks only initial material, maximum 3600')
parser.add_argument('--natural-profile', choices=['current', 'adapted'], default='current', help='Preset for the natural village survey')
mode.add_argument('--natural', action='store_true', help='Generate natural starts and complete two villages')
parser.add_argument('--generation-config', type=Path, help='Explicit candidate geometry profile; installed config remains read-only')
parser.add_argument('--interior-config', type=Path, help='Explicit candidate interiors; installed config remains read-only')
parser.add_argument('--water-config', type=Path, help='Explicit local-water candidate; installed config remains read-only')
parser.add_argument('--ocean-config', type=Path, help='Explicit ocean habitat candidate; installed config remains read-only')
parser.add_argument('--seed', type=int, default=1)
parser.add_argument('--terrain-seed', type=int, help='Survey this exact seed instead of the five-seed terrain comparison')
parser.add_argument('--maximum-gap', type=int, default=512)
parser.add_argument('--disable-carver-adapter', action='store_true',
                    help='Diagnostic control: omit relative-carving mixins from the staged mod only')
args = parser.parse_args()
java_path = shutil.which(args.java)
if not java_path:
    parser.error(f'Java executable not found: {args.java}')
args.java = str(Path(java_path).resolve())
if args.dh_stop_after and (not args.dh_pregen or not 1 <= args.dh_stop_after <= 30):
    parser.error('--dh-stop-after requires --dh-pregen and 1 to 30 seconds')
if args.dh_world_key and not args.dh_pregen:
    parser.error('--dh-world-key requires --dh-pregen')
if args.terrain_seed is not None and not -(1 << 63) <= args.terrain_seed < (1 << 63):
    parser.error('--terrain-seed must be a signed 64-bit integer')
if args.terrain_seed is not None and not args.terrain:
    parser.error('--terrain-seed requires --terrain')
if args.fluid_ticks < 0 or args.fluid_ticks > 3600:
    parser.error('--fluid-ticks must be between 0 and 3600')
if args.oceans and not args.ocean_config:
    parser.error('--oceans requires --ocean-config')
if args.minimal and args.generation != 'adapted' and not any((args.coverage, args.structures, args.surface_structures, args.overlap, args.interiors, args.veins, args.water, args.materials, args.oceans)):
    parser.error('--minimal requires a terrain, coverage, structure, interior, vein, water or material check')
if args.reopen and not (args.generation or args.natural):
    parser.error('--reopen requires --generation or --natural')
if args.natural_profile != 'current' and not args.natural:
    parser.error('--natural-profile requires --natural')
server = args.server.expanduser().resolve()
version = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
mod = ROOT / f'build/libs/create-sky-compat-{version}-mc1.21.1-neoforge.jar'
if args.mod:
    mod = args.mod.expanduser().resolve()
fixture = ROOT / 'build/probe/create-sky-compat-probe.jar'
output = ROOT / 'build/verification'
output.mkdir(parents=True, exist_ok=True)
name = f'islands-{args.seed}' if args.islands else f'mechanics-{args.seed}' if args.mechanics else f'biomes-{args.seed}' if args.biomes else f'generation-{args.generation}-{args.seed}' if args.generation else 'terrain' if args.terrain else f'natural-{args.natural_profile}-{args.seed}' if args.natural else 'placement'
if args.terrain_seed is not None: name = f'terrain-{args.terrain_seed}'
if args.structures: name = f'structures-{args.seed}'
if args.surface_structures: name = f'surface-structures-{args.seed}'
if args.coverage: name = f'coverage-{args.seed}'
if args.benchmark: name = f'benchmark-{args.seed}'
if args.overlap: name = 'overlap'
if args.interiors: name = f'interiors-{args.seed}'
if args.veins: name = f'veins-{args.seed}'
if args.water: name = f'water-{args.seed}-{args.fluid_ticks}'
if args.materials: name = f'materials-{args.seed}'
if args.oceans: name = f'oceans-{args.seed}-{args.fluid_ticks}'
if args.surface_climate: name = f'surface-climate-{args.seed}'
if args.chunk_futures: name = f'chunk-futures-{args.seed}'
if args.dh_pregen: name = f'dh-pregen-{args.seed}'
if args.dh_stop_after: name += '-shutdown'
if args.dh_world_key: name += '-world-key'
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
    if args.dh_pregen:
        (run / 'defaultconfigs').mkdir(exist_ok=True)
        (run / 'defaultconfigs/create_sky_compat-dh-server.toml').write_text(
            f'savedWorldIdentity = {str(args.dh_world_key).lower()}\n')
    # Native probes do not serve maps or download/render map assets.
    # Only disposable configuration is edited; the integration still class-loads.
    bluemap = run / 'config/bluemap'
    if bluemap.is_dir():
        (bluemap / 'webserver.conf').write_text('enabled: false\n')
        (bluemap / 'core.conf').write_text('accept-download: false\nmetrics: false\n')

    (run / 'config/create_sky_compat-common.toml').write_text(
        f'enabled = true\nminimumGap = 64\nmaximumGap = {args.maximum_gap}\nminimumIslandRadius = 96\n')
    if args.generation_config:
        shutil.copy2(args.generation_config.expanduser().resolve(), run / 'config/create_sky_compat-generation.json')
    if args.interior_config:
        shutil.copy2(args.interior_config.expanduser().resolve(), run / 'config/create_sky_compat-interiors.json')
    if args.water_config:
        shutil.copy2(args.water_config.expanduser().resolve(), run / 'config/create_sky_compat-water.json')
    if args.ocean_config:
        shutil.copy2(args.ocean_config.expanduser().resolve(), run / 'config/create_sky_compat-oceans.json')
    # Reuse the owner's existing EULA acceptance; never create acceptance implicitly.
    shutil.copy2(server / 'eula.txt', run / 'eula.txt')
    preset = {'current': 'sky_archipelago:sky_islands', 'adapted': 'create_sky_compat:islands_v1', 'vanilla': 'minecraft:normal'}.get(args.generation, ('create_sky_compat:islands_v1' if args.natural_profile == 'adapted' else 'sky_archipelago:sky_islands') if args.natural else 'minecraft:flat')
    if args.structures or args.surface_structures or args.coverage or args.benchmark or args.biomes or args.surface_climate or args.mechanics or args.islands or args.chunk_futures or args.dh_pregen: preset = 'create_sky_compat:islands_v1'
    if args.interiors or args.veins or args.water or args.materials or args.oceans: preset = 'create_sky_compat:islands_v1'
    (run / 'server.properties').write_text('\n'.join([
        'server-ip=127.0.0.1', 'server-port=0', 'online-mode=false',
        'level-name=probe-world', f'level-type={preset}', f'level-seed={args.seed}',
        f'generate-structures={str(args.natural or args.chunk_futures or args.structures or args.surface_structures or args.dh_pregen).lower()}', 'view-distance=2',
        'max-tick-time=0', '',
    ]))
    launch_args = next((server / 'libraries/net/neoforged/neoforge').glob('*/unix_args.txt'))
    command = [args.java, f'-Xmx{args.heap}',
               f'-Dneighbors.probe.dhStopAfter={args.dh_stop_after}',
               f'-Dneighbors.probe.dhWorldKey={str(args.dh_world_key).lower()}',
               f'-Dneighbors.probe.generation={args.generation or ""}',
               f'-Dneighbors.probe.structures={str(args.structures or args.surface_structures).lower()}',
               f'-Dneighbors.probe.surfaceStructures={str(args.surface_structures).lower()}',
               f'-Dneighbors.probe.coverage={str(args.coverage).lower()}',
               f'-Dneighbors.probe.benchmark={str(args.benchmark).lower()}',
               f'-Dneighbors.probe.surfaceClimate={str(args.surface_climate).lower()}',
               f'-Dneighbors.probe.biomes={str(args.biomes).lower()}',
               f'-Dneighbors.probe.mechanics={str(args.mechanics).lower()}',
               f'-Dneighbors.probe.islands={str(args.islands).lower()}',
               f'-Dneighbors.probe.chunkFutures={str(args.chunk_futures).lower()}',
               f'-Dneighbors.probe.dhPregen={str(args.dh_pregen).lower()}',
               f'-Dneighbors.probe.terrain={str(args.terrain).lower()}',
               *([f'-Dneighbors.probe.terrainSeed={args.terrain_seed}'] if args.terrain_seed is not None else []),
               f'-Dneighbors.probe.overlap={str(args.overlap).lower()}',
               f'-Dneighbors.probe.interiors={str(args.interiors).lower()}',
               f'-Dneighbors.probe.veins={str(args.veins).lower()}',
               f'-Dneighbors.probe.water={str(args.water or args.oceans).lower()}',
               f'-Dneighbors.probe.oceans={str(args.oceans).lower()}',
               f'-Dneighbors.probe.materials={str(args.materials).lower()}',
               f'-Dneighbors.probe.fluidTicks={args.fluid_ticks}',
               f'-Dneighbors.probe.expectedMaximumGap={args.maximum_gap}',
               f'-Dneighbors.probe.natural={str(args.natural).lower()}',
               f'-Dneighbors.probe.samples={args.samples}', f'-Dneighbors.probe.output={report}',
               '@' + str(launch_args), 'nogui']
    began = time.monotonic()
    def launch(log_path):
        with log_path.open('w') as log:
            subprocess.run(command, cwd=run, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=90 if args.dh_stop_after else 600)
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
        data['peakResidentBytes'] = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss * (1 if sys.platform == 'darwin' else 1024)
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
if args.structures or args.surface_structures:
    print('Natural structure coverage:', report)
    raise SystemExit(0)
if args.coverage:
    print('Resolved worldgen coverage:', report)
    raise SystemExit(0)
if args.chunk_futures:
    print('Chunk future comparison:', report)
    raise SystemExit(0)
if args.dh_pregen:
    print('Disposable DH generation:', report)
    raise SystemExit(0)
if args.mechanics:
    print('Native mechanics survey:', report)
    raise SystemExit(0)
if args.islands:
    print('Island resource survey:', report)
    raise SystemExit(0)
if args.benchmark:
    print('Native bounded benchmark:', report)
    raise SystemExit(0)
if args.surface_climate:
    print('Surface climate survey:', report)
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
if args.overlap:
    print('Native overlap comparison:', report, 'seconds:', data['elapsedSeconds'])
    raise SystemExit(0)
if args.interiors:
    print('Native interior sections:', report, 'seconds:', data['elapsedSeconds'])
    raise SystemExit(0)
if args.veins:
    print('Native vein witnesses:', report, 'seconds:', data['elapsedSeconds'])
    raise SystemExit(0)
if args.water or args.oceans:
    print('Native water check:', report, 'seconds:', data['elapsedSeconds'])
    raise SystemExit(0)
if args.materials:
    print('Native material check:', report)
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

# Development

Use JDK 21 and the checked-in Gradle wrapper:

```sh
./gradlew --no-daemon check build
```

The target is Minecraft 1.21.1 / NeoForge 21.1.264. Sky Archipelago 1.3.6 and
Sky Villages 1.0.6 are pinned by Modrinth version ID and SHA-256. They are downloaded
for compilation/testing, not redistributed. Architectury 13.0.11 is included in
the development runtime for Sky Villages. Output is in `build/libs`.

See [DH integration](lod.md) for optional lifecycle and cache fixes, pinned
implementation boundaries and dependency-update checks. The server and client
probes are separate artifacts and are never bundled in the mod.

For an isolated full-pack shutdown check with DH generation active:

```sh
./gradlew probeJar
python3 tools/probe.py --dh-pregen --dh-stop-after 2 --seed 42 \
  --server /path/to/server/game --java /path/to/java
```

The input server is read-only. The fixture uses a temporary save and a local random
port, requests a bounded DH job, and asks the fixture server to stop while the job
is active. A normal process exit is required within 90 seconds. Add `--dh-world-key`
to exercise opt-in UUID cache naming; without it the fixture requires ordinary keys
and no new identity marker. These commands never submit work to the input server.

The [client probes](#client-probes) cover legacy-cache construction and known
fluid/material inputs. Scalar measurements do not replace native-versus-LOD
visual comparison.

## Implementation

`OptionalMixinPlugin` adds integration mixins only after checking NeoForge's early
loading metadata. Sky Villages hooks require both `skyvillages` and `architectury`;
Sable and ADQ hooks require their respective mod IDs. The plugin references no
game or optional-mod classes. Required injection counts remain enforced when an
integration is selected. Runtime village fallback and saved-world identity use
the same presence predicate. The weather mixin is client-only; configuration UI
registration is also client-only.

The Sable API is compile-only with a separate development runtime dependency.
Development runs retain the complete pinned integration set. For the minimal
native check, run:

```sh
python3 tools/probe.py --generation adapted --minimal --seed 42 --reopen --heap 6G \
  --server /path/to/server/game --java /path/to/java
```

It stages only Sky Archipelago, Compat and the separate probe, without copying
other mods' configs.
The full check omits `--minimal`. These cover the general gameplay integration set. The optional BlueMap boundary
has a separate minimal-plus-BlueMap check described in [BlueMap](bluemap.md).

Loading references: [NeoForge metadata](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/)
and [Mixin configuration plugins](https://javadoc.io/static/net.fabricmc/sponge-mixin/0.16.4%2Bmixin.0.8.7/org/spongepowered/asm/mixin/extensibility/IMixinConfigPlugin.html).

[Island generation](terrain.md) owns the expanded preset, geometry, geological
frame, saved-world compatibility and native terrain survey. The following describes
the retained village-neighbor predicate.

`placement/NeighborRule` owns the loader-independent geometry. `IslandTerrain`
adapts Sky Archipelago's resolved column field; it never requests world chunks.
`StructureSupportMixin` filters accepted SKY reports for exactly
`skyvillages:skyvillage`. Native height fitting happens before this hook; overlap
reservation, registration and structure references happen afterward.

The rule first rejects obvious overlap on a coarse grid, then finds a real nearby
terrain sample with a qualifying dominant island descriptor. Accepted candidates
must additionally pass an exhaustive column check in the minimum-clearance band.
All distances use the complete layout's X/Z bounding rectangle. Bounding-box gaps
between buildings are deliberately reserved too. This predicate introduces no mutable placement counters or visitation-order
choices. The dedicated preset separately applies the deterministic per-archipelago
allowance described in the island-generation document.

This predicate is deterministic for the supplied layout, terrain field and
settings. It does not remove upstream's existing structure overlap reservations
or prove that the entire upstream generation pipeline is independent of order.
Debug logging distinguishes `TOO_CLOSE` and `NO_HOST`; the upstream engine may also
report its generic air-clearance rejection stage.

`NeighborConfig` registers a NeoForge COMMON spec and captures immutable settings
at server startup. A client-only entry point registers NeoForge's config screen.
The server artifact has no dependency on loading client GUI classes.

## Analog Audio

`AnalogAudioCreateMixin` corrects one reflective lookup in Analog Audio
0.1.5-hotfix.1 with Create 6.0.10. Upstream asks the package-private registry
implementation for `register(Block, MovementBehaviour)`. Java erases those generic
parameters to `Object`; obtaining the method from Create's public `SimpleRegistry`
interface also avoids illegal access to its implementation. The adapter preserves
upstream setup timing, handler construction, registration and error handling.

Loader metadata selects the hook only for that exact pair. Neither mod becomes a
required or compile-time dependency, and no client classes enter the dedicated
server path. Other versions retain their upstream behavior. This adapter changes
no recipes, resources, weights, radio controls or Sable coordinate handling.

Before changing either dependency, inspect `CreateCompat.lambda$onCommonSetup$1`,
`SimpleRegistry.register` and the upstream `RadioMovementBehaviour`. Remove the
hook if upstream fixes registration; otherwise update its exact gate only after
rechecking the lookup and required injection count. Do not replace the upstream
handler or broaden GUI behavior as part of this fix.

Build with `./gradlew check build probeJar`, then run
`python3 tools/probe.py --audio SELECTION --server /path/to/server/game --java /path/to/java`
for `minimal`, `create`, `analog`, `pair` and `full`. These modes check optional
class loading, the actual radio registration, a control Create drill handler,
and dedicated-server callbacks. Use `--mod /path/to/baseline.jar --audio pair`
for the expected failing control. Reports include exact artifact hashes.
Client playback, moving sound positions, stopping/disassembly and GUI controls
require a separate interactive check; successful registration does not prove them.

## Verification

`tools/probe.py --coverage` exports resolved Overworld biome feature lists, placed
and configured features, structures, structure sets, resource priority and bounded
ancient-city eligibility observations. `--structures --heap 6G` requests natural
underground structure candidates and realizes the first bounded layout per type.
`--surface-structures` does the same for vanilla surface/water families. These modes
accept `--minimal`; generated JSON records scope and artifact hashes. Full-set
structure captures wait for a loaded neighborhood and ticks to inspect one Lootr
conversion. Generation captures disable only their disposable BlueMap webserver
and asset-download configuration. They do not test map rendering. Counts
of accepted starts are candidate observations, not natural occurrence rates.
`tools/feature_coverage.py CAPTURE --output CLOSURE` traces registered and inline
nested placements from that capture. It distinguishes placed/configured IDs and
reports height modifiers under selectors; update its typed traversal if new feature
codecs add nested placements.


`--terrain` checks native descriptors and reordered/concurrent queries across
five seeds. Use `--terrain --terrain-seed SEED` to reproduce a particular world's
layout with its installed configuration instead. This estimates terrain from the generator,
rather than reading stored chunks; compare selected points with the saved world before
making claims about its contents. Compare its reports with `tools/layout_report.py` (matplotlib), which
draws descriptor radii at equal scale rather than pretending they are shorelines.
`--overlap --minimal` compares exterior intervals under VOID/OVERLAP at identical
geometry and three seeds. `--interiors --minimal --seed N` records representative
baseline/noise/hollow cross-sections, solid-volume differences and independent
snapshot/direct query-order checks. These are pre-carver baseline samples, not
observations of decorated chunks or dynamic water. Use `--generation adapted`
for actual fill/base-column agreement and completed chunk content.
`--veins --minimal --seed N` searches a bounded set of solid columns for raw
iron/copper witnesses, then checks native fill, base columns and completed chunks.
This deliberately targeted search does not estimate vein abundance. A missing
witness fails its completion check without establishing global absence.
`--surface-climate --seed 42` inspects contiguous warm, cold and transition areas
before and after an explicit native precipitation sweep. `--benchmark --seed 42`
measures a fixed column/chunk workload; use `--mod /exact/baseline.jar` for a
comparison without modifying installed instances. Different layouts generate
different terrain, so timings do not isolate the cost of a single algorithm.

`check` runs geometry tests and verifies upstream artifact hashes. Tests cover
void, fragments, exact distance boundaries, narrow unaligned overlaps, full
footprints, invalid ranges, negative/world-border coordinates and concurrent
calls. CI runs checks/builds on Linux, Windows and macOS. Native generation is a
separate opt-in check; CI unit tests are not a substitute for it.

To test with an installed server's exact mod selection and config:

```sh
./gradlew --no-daemon check build probeJar
python3 tools/probe.py --server /path/to/server/game --java /path/to/java
python3 tools/probe.py --natural --seed 1 --server /path/to/server/game --java /path/to/java
```

Use `--generation-config /path/to/create_sky_compat-generation.json` to stage an
authored candidate profile instead of the installed one. Strict profile parsing
rejects obsolete fields; do not accidentally test against an older installed JSON.
`--interior-config /path/to/create_sky_compat-interiors.json` similarly stages
an explicit cavity/carver comparison without changing the installed instance.

The runner reads installed libraries, mods, config and existing EULA acceptance.
It creates a disposable world, binds loopback on an ephemeral port, and removes
the temporary server on exit. It never mounts a live save. The probe waits for the first completed server tick, after all startup spawn
handlers. Its JAR is separate from release artifacts. Results go to `build/verification`.

`--dh-pregen --seed N --heap 6G` runs the installed DH dedicated pregeneration
manager in a disposable full-installation world. It uses the command's minimum
32-chunk radius, waits on that isolated job's future, records region-header entries
before/after, and removes the test world. This mode cannot run with `--minimal`.
It is a development probe, not a monitor for an operator's server or pregen task.

`--islands --seed N --heap 6G` samples three anchors using 49 spatial strata each. It records
observed blocks, weighted estimates, relative ore depths, spawn collision checks,
surface biomes, descriptors and generation/scanning time. Reports are JSON and
can be consumed by external analysis tools. See
[terrain](terrain.md) for sampling limits.

The default heap is 4 GiB; `--heap` accepts 4G, 6G or 8G. The report records the
selected limit and observed end-of-scan heap usage. The runner rejects fatal
runtime and shutdown errors even when Minecraft exits zero after writing a report.
End-of-scan usage is not peak memory or a recommended production allocation.

`--mechanics` calls the installed balloon search and assembly/disassembly helpers
at boundary heights. It allows normal ticks for deferred physics cleanup before
stopping. Use a separate interactive aircraft fixture for flight and persistence.

`--chunk-futures --seed 42 --heap 6G` compares background FULL requests, explicit
retained-ticket controls and the production delivery adapter. The last case
invokes ADQ's applied rejection handler and checks that the lease is released.
This diagnoses endpoint loading; it does not transport or complete cargo.

### Client probes

`./gradlew clientProbeJar` builds `build/probe/create-sky-client-probe.jar`.
Install it only in a disposable client, with desktop authorization before launch.
It is excluded from production artifacts and the server probe.

With DH 3.3.3 installed, JVM flags select these checks:

| Flag | Behavior and output |
| --- | --- |
| `-Dcreate_sky_compat.probe.dhCache=true` | Exercises the real DH client constructor against temporary legacy/current folders; writes `lod-cache-client-probe.json`. Does not open a world. |
| `-Dcreate_sky_compat.probe.dhFluid=true` | Creates or reopens `dh-fluid-review`, places lava/water and stone/lichen fixtures, and writes `dh-fluid-colors.json` with exact material/color measurements and lichen-transfer controls. |

Both flags may be used together. For the fluid scene, disable background DH
generation in the disposable configuration, visit the columns at X=0..81, Z=0,
Y=500..700, then view them beyond native render distance. Compare day/night,
shaders off/on and a normal save/reopen. The fixture retains native fluid ticks.
Changing a block exclusion should be tested against the same persisted LODs,
not a cleared database. See [DH behavior](lod.md#luminous-columns-and-fluid-colors).

ModDev's `runClient` cannot locate Connector's clean Minecraft artifact in the
tested setup. The renderer check excluded Connector, Surveyor and Hoofprint;
this is not complete-pack launcher coverage. When connecting a development
client to an isolated production server containing owo-lib, match its handshake
mode with `-Dowo.handshake.enabled=true` on that test server. Do not alter a
playing server for a harness-only mismatch.

Without either DH flag, the probe logs the dimension envelope, Sable pressure,
altitude and tracked sub-level bounds every five seconds. That mode writes no
world state. Telemetry does not replace checking aircraft construction and controls.
Close the selected client process normally after recording results.

For the complete Create Sky development stack, the separate
`-Dcreate_sky_compat.probe.compatibility=true` mode opens `compatibility-review`,
a disposable flat-world entity fixture. It exports live Sable block properties,
checks seven assembled building-material fixtures, and samples natural terrain
models through DH's real conversion path. It requires DH, Sable, Simulated and
the fixture's building mods; it is not a minimum-install probe. The JSON outputs
are `block-mass.json`, `mass-assemblies.json` and `dh-terrain-materials.json`.
Visual animation checks still require desktop observation and short recordings.

The layout probe exercises the loaded Mixin against native village layouts across
four seeds and compares maximum-distance variants. The natural probe checks saved
starts against the predicate and fully generates two retained villages. It counts
realized blocks, rolls their actual chest tables without a player, and checks ADQ's
native surface resolver through the applied compatibility handler. Loot samples
are not Lootr player entitlements or a completed delivery. Global
Mixin debug expectation counting is not enabled: it changes optional third-party
injection validation in the tested pack. This mod's injection remains required,
and the fixture asserts its actual rejection behavior.

Before release, rerun native checks after changes to upstream dependencies or the
predicate. Update the [verification evidence](verification.md), inspect package
contents and test the release staging process. Keep downloaded classes, worlds,
logs and inspection files out of Git.

# Development

Use JDK 21 and the checked-in Gradle wrapper:

```sh
./gradlew --no-daemon check build
```

The target is Minecraft 1.21.1 / NeoForge 21.1.264. Sky Archipelago 1.3.6 and
Sky Villages 1.0.6 are pinned by Modrinth version ID and SHA-256. They are downloaded
for compilation/testing, not redistributed. Architectury 13.0.11 is included in
the development runtime for Sky Villages. Output is in `build/libs`.

See [distant terrain integration](lod.md) for DH/SeedGen/Voxy findings and the
proposed optional terrain provider; it is not part of the released mod.

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
layout with its installed configuration instead. This remains a native prediction,
not a read of stored terrain; compare selected points with the saved world before
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

`./gradlew clientProbeJar` builds optional read-only client instrumentation in
`build/probe/create-sky-client-probe.jar`. Install it only in a disposable client.
Every five seconds it logs the loaded dimension envelope, Sable pressure profile,
sampled pressure values, player altitude and tracked sub-level bounds. It writes
no world state and is not packaged with either production mod or the server probe.
Compare its observations with the dedicated server and native captures; telemetry
does not replace checking the aircraft's construction and controls.

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

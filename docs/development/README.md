# Development

Use JDK 21 and the checked-in Gradle wrapper:

```sh
./gradlew --no-daemon check build
```

The target is Minecraft 1.21.1 / NeoForge 21.1.264. Sky Archipelago 1.3.6 and
Sky Villages 1.0.6 are pinned by Modrinth version ID and SHA-256. They are downloaded
for compilation/testing, not redistributed. Architectury 13.0.11 is included in
the development runtime for Sky Villages. Output is in `build/libs`.

## Implementation

`placement/NeighborRule` owns the loader-independent geometry. `IslandTerrain`
adapts Sky Archipelago's resolved column field; it never requests world chunks.
`StructureSupportMixin` filters accepted SKY reports for exactly
`skyvillages:skyvillage`. Native height fitting happens before this hook; overlap
reservation, registration and structure references happen afterward.

The rule first rejects obvious overlap on a coarse grid, then finds a real nearby
terrain sample with a qualifying dominant island descriptor. Accepted candidates
must additionally pass an exhaustive column check in the minimum-clearance band.
All distances use the complete layout's X/Z bounding rectangle. Bounding-box gaps
between buildings are deliberately reserved too. No mutable placement counters,
random rolls, cluster quotas or visitation-order choices are introduced.

This predicate is deterministic for the supplied layout, terrain field and
settings. It does not remove upstream's existing structure overlap reservations
or prove that the entire upstream generation pipeline is independent of order.
Debug logging distinguishes `TOO_CLOSE` and `NO_HOST`; the upstream engine may also
report its generic air-clearance rejection stage.

`NeighborConfig` registers a NeoForge COMMON spec and captures immutable settings
at server startup. A client-only entry point registers NeoForge's config screen.
The server artifact has no dependency on loading client GUI classes.

## Verification

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

The runner reads installed libraries, mods, config and existing EULA acceptance.
It creates a disposable world, binds loopback on an ephemeral port, and removes
the temporary server on exit. It never mounts a live save. The probe JAR is separate
from release artifacts. Results go to `build/verification`.

The layout probe exercises the loaded Mixin against native village layouts across
four seeds and compares maximum-distance variants. The natural probe checks saved
starts against the predicate and fully generates two retained villages. Global
Mixin debug expectation counting is not enabled: it changes optional third-party
injection validation in the tested pack. This mod's injection remains required,
and the fixture asserts its actual rejection behavior.

Before release, rerun native checks after changes to upstream dependencies or the
predicate. Update the [verification evidence](verification.md), inspect package
contents and test the release staging process. Keep downloaded classes, worlds,
logs and inspection files out of Git.

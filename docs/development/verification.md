# Verification

Checked on 2026-10-23 with Java 21, Minecraft 1.21.1, NeoForge 21.1.264,
Sky Archipelago 1.3.6 and Sky Villages 1.0.6. Native runs used Create Sky 0.1.8's
installed server mods/config as read-only inputs, with disposable saves.

Tested mod JAR SHA-256:
`16ddc1ba1ece7b81726ca3767661717da8cef072e80b256f5230545bc67422e1`.

## Build and release checks

`./gradlew --no-daemon check build probeJar` passed. This covered 12 geometry
unit tests, both upstream SHA-256 checks, packaged dependency ranges and absence
of bundled upstream/fixture classes. Five release-tool tests passed, including
cross-platform artifact mismatch, missing artifact, tag/version mismatch and wrong
checkout rejection. The staging test uses synthetic build files; it does not prove
that three operating systems produced identical real binaries.

`mise x aqua:rhysd/actionlint@1.7.12 -- actionlint .github/workflows/build.yml`
passed. Tag publication requires Linux, Windows and macOS CI checks and matching
binary/source JARs; the release workflow enforces that gate.

## Native placement

`python3 tools/probe.py --server <game> --java <java>` exercised 48 native layouts
across seeds 1, 42, 8675309 and 424242. Of these, 42 passed the unfiltered native SKY
clearance check. The loaded Mixin rejected 28 at the default distance and retained
14, matching the standalone predicate. Unrelated SKY structure IDs retained their
native acceptance, and layout bounds were unchanged.

| Maximum distance | Retained of 42 | Too close | No qualifying host |
| --- | --- | --- | --- |
| 384 | 5 | 6 | 31 |
| 512 | 14 | 6 | 22 |
| 768 | 16 | 6 | 20 |

This supports 512 as a starting default: 384 discarded considerably more candidates,
while 768 admitted only two more at longer distances. These are layout samples,
not a biome-adjusted village density or a guaranteed count per cluster.

For the 512-block variant, median predicate time was 11.7 ms and maximum was
499.2 ms on the test machine. Variants ran in ascending distance order and reused
the terrain field's caches. This is a bounded measurement, not a cold-cache
pregen benchmark or a performance guarantee for large custom settings.

Natural runs used `tools/probe.py --natural --seed <seed>`; the third run also
used `--maximum-gap 384`, verifying a non-default TOML value at server startup.
Each examined 49 random-spread candidate chunks and fully generated two retained
villages, including all chunks intersecting their bounds.

| Seed | Maximum gap | Retained starts | Fully generated villages |
| --- | --- | --- | --- |
| 1 | 512 | 14 | 2 |
| 42 | 512 | 8 | 2 |
| 8675309 | 384 | 10 | 2 |

Every retained start passed the predicate and world-height bounds. The six complete
villages contained 18,753 to 32,812 non-air blocks across 58 to 85 pieces. The test
checks realization, not individual loot correctness or a visual playtest. The
natural runs include native biome checks and the pack's other worldgen mods.

No live save was opened, regenerated or synchronized. Temporary servers were
removed after each run. Client config-screen interaction and a client joining
without the patch were not exercised; the patch registers no network payloads or
new game registries and keeps its GUI entry point client-only. Upstream generation
order guarantees and Distant Horizons previews were not tested here.

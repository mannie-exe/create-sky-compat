# Verification

## Terrain pass — 2026-10-25

Minecraft 1.21.1, NeoForge 21.1.264 and Java 21.0.13 on macOS/aarch64.
Full runs use Create Sky 0.1.13 with the candidate substituted; minimal runs contain
only Sky Archipelago, Compat and the probe. All probe worlds are disposable.
The playing world was never mounted.

Version 0.4.0 binary SHA-256:
`8b9d52d9eb07f05b0c2cb448bd927fd12152d9fafa8afd2aa00b47a4d1f8ec9f`.
Final terrain, full/minimal reopening and seed-8675309 resources use this binary.
Earlier seed-42 resources, villages and client checks used the candidate under
version 0.3.0. Class-byte comparison found only GenerationGuard changed and the
two spawn correction classes added; their terrain and integration classes match.
Each native report records its own mod/probe hashes.

Commands used, with `--server /path/to/server/game --java /path/to/java --heap 6G`
and `--generation-config /path/to/create_sky_compat-generation.json` on each probe:

```sh
./gradlew --no-daemon check build probeJar
python3 tools/probe.py --terrain --samples 4
python3 tools/probe.py --generation adapted --seed 42 --reopen
python3 tools/probe.py --generation adapted --minimal --seed 42 --reopen
python3 tools/probe.py --islands --seed 42
python3 tools/probe.py --islands --seed 8675309
python3 tools/probe.py --natural --natural-profile adapted --seed 42 --reopen
```

Build/package/upstream checks passed. Full and minimal installations each matched
4,128,768 noise-fill/base-column positions and retained sampled ore counts after
reopening. Climate scope checks passed; the full run also passed Sable pressure
and codec checks. Intermediate optional-mod combinations were not rerun.

## Geometry

Five seeds cover native layouts within 16,000 blocks and clearance measurements
inside 10,000. A 16-block footprint grid sampled four main islands per seed:
targets were 259–397 blocks, realized density extents 247–395. These exclude trees
and structures. Finite samples can underestimate extremes; targets are approximate
because native relief and erosion remain. Complete-cluster floor/headroom checks
passed. The mathematical support guarantee is 354 blocks, independent of samples.

| Seed | Interior anchors | Center Y | Minimum support gap | Sampled shore gaps |
| --- | ---: | --- | ---: | --- |
| 1 | 35 | 206–480 | 645 | 1168–1441 |
| 42 | 31 | 200–483 | 679 | 1264–1647 |
| 8675309 | 22 | 206–479 | 735 | 2397–2602 |
| 424242 | 30 | 203–489 | 576 | 1072–1350 |
| -3095298318114116683 | 37 | 218–480 | 517 | 850–1209 |

Values are blocks, rounded. Shore measurements include companions/spires but use
three nearest-center pairs per seed; they are neither exact nearest-shore proofs
nor navigated journeys. Biome exclusions and activation can leave longer gaps.

The old spawn descriptor reproduces the native density jump at X1800/Z1928:
Y44 is +0.131210 and Y43 is −0.480853. With the new underside evaluator the
one-block change is 0.0125 instead of 0.612063. This isolates the discontinuity;
new worlds also use new randomized descriptors.

A disposable shader-off client inspected generated crescent and bowl terrain on
seed -3095298318114116683. The crescent near (1114,342,1165) had a curved,
terraced underside rather than a broad flat slice. The bowl near (1193,300,-1114)
retained its cavity, broken rim, deepslate and waterfalls. The initially partly
loaded distant bowl view was excluded. These views do not prove every archetype
or quantify the volume removed by the outer support guard.

## Resources and spawn

Each anchor uses 49 spatial strata. Counts combine ordinary/deepslate ores and
are observed blocks in sampled anchor-owned columns, not complete inventories.
The Atlas retains strata, weighted estimates and source hashes separately.

| Seed / center Y | Observed block Y | Zinc | Diamond | Platinum | Surface-opening columns |
| --- | --- | ---: | ---: | ---: | ---: |
| 42 / 337 | 59–399 | 557 | 94 | 102 | 0 |
| 42 / 466 | 159–515 | 319 | 21 | 48 | 0 |
| 42 / 269 | 96–319 | 294 | 43 | 30 | 0 |
| 8675309 / 354 | 124–404 | 215 | 11 | 9 | 0 |
| 8675309 / 304 | 94–351 | 212 | 7 | 19 | 0 |
| 8675309 / 292 | 61–344 | 187 | 25 | 32 | 9 |

All six also contained iron, copper, coal, redstone and andesite. No emerald was
observed; sparse sampling does not establish absence or a broken biome rule.
Resource presence does not establish mining access, quantities per journey or
survival scarcity. No recipe or ore-count changes were justified by these samples.

Surface-opening counts require generated air eight blocks into predicted surface
rock. Five samples had none; this does not prove no entrances exist, and the nine
columns on the sixth do not prove deep connectivity. Cave access remains a
playtest concern. The existing geological/carver adapters remain in place.

Seed 8675309 exposed an unsupported native fallback spawn at (-926,387,1038).
The generated-surface correction placed it at (-926,379,1038), with stone support
and clear feet/head. The final resource probe passed its spawn assertions.
The correction is confined to the native fresh-world fallback; it does not
relocate existing worlds or manufacture a platform.

## Villages and integrations

Seed 42 planned 52 anchors, with target allowances `[38,9,4,1]` for zero through
three villages. Layout/biome/clearance filtering accepted 13 starts; two were fully
generated. Reverse-order planning and reopening matched. Their native ADQ locate
points failed surface checks; the bounded adapter found safe landings at
(-1922,401,-1134) and (-1058,410,-2126). Scope and horizontal-distance controls
passed. These are endpoints, not completed deliveries or route budgets.

The unchanged pressure curve remains continuous in world altitude: 1.3 at Y180,
1.15 at 400, 1.0 at 508, 0.8 at 600 and zero at 768. The earlier release's
powered, glued balloon completed an outward/return cargo crossing and save/reopen
check. That remains historical evidence, not a new-grid flight test.

The previous pass verified projected cave biomes, representative underground
features, cold/temperate classification, ticketed ADQ requests and shader weather.
Those adapters are unchanged; their detailed implementation contracts live in
[terrain](terrain.md). They were not all rerun here. Natural fossil frequency,
ancient-city/stronghold realization and complete shallow/deep cave connectivity
remain unverified. No unrelated pressure, shader or physics changes were made.

## DH and limits

A real isolated dedicated server completed a 32-chunk-radius DH pregen centered
at (1114,1165) with the visual client connected. Its log contained completion and
no expired-request message. This is a bounded generator check, not a guarantee
for overnight runs. A prior release's isolated run experienced transient expired
requests before completion; its cause remains unresolved.

Generation JSON, density/spawn behavior, mappings and optional integrations
participate in saved-world identity. Never remove a marker to adopt incompatible
chunks. Nether/End and ordinary-preset scope are retained; full foreign-world
content surveys and a new survival flight/mining run were not performed.

Keep owning scripts, imported measurements and docs; remove disposable worlds,
captures and raw logs. Recheck pinned native internals when updating Sky
Archipelago, village layouts, ADQ private methods or Sable pressure records.

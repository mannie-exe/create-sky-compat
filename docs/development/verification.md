# Verification

## Terrain pass — 2026-10-25

Minecraft 1.21.1, NeoForge 21.1.264, Java 21.0.13 on macOS/aarch64.
Full runs use the installed Create Sky 0.1.12 mod selection with this candidate
substituted; minimal runs contain only Sky Archipelago, Compat and the probe.
All generated worlds are disposable. The playing server was not mounted.

Release-candidate binary SHA-256: `36f990b71bbddc31aff980f186b13a80265ddbc7017383c734d7673efc224c1d`.
Earlier terrain, village, biome and client runs used the same production classes
under version 0.2.0; ZIP comparison found only `META-INF/neoforge.mods.toml`
different in the 0.3.0 binary. Full/minimal reopening and the final seed-8675309
resource/future checks used 0.3.0 itself. Each report records mod/probe hashes.

Commands used, with `--server /path/to/server/game --java /path/to/java --heap 6G`
on each native probe:

```sh
./gradlew --no-daemon check build probeJar clientProbeJar
python3 tools/probe.py --terrain --samples 4
python3 tools/probe.py --generation adapted --seed 42 --reopen
python3 tools/probe.py --generation adapted --minimal --seed 42 --reopen
python3 tools/probe.py --islands --seed 42
python3 tools/probe.py --islands --seed 8675309
python3 tools/probe.py --biomes --seed 42
python3 tools/probe.py --natural --natural-profile adapted --seed 42 --reopen
python3 tools/probe.py --chunk-futures --seed 42
```

Build/package/upstream checks passed. Full and minimum installations each matched
4,128,768 noise-fill/base-column positions, retained sampled ore counts after
reopening, and passed climate scope checks. Optional integrations reported present
only in the full run. The full run also passed the unchanged Sable pressure curve
and codec checks. Intermediate optional-mod combinations were not rerun.

## Layout and resources

The native layout query covers a 16,000-block radius, with interior clearance
measurements inside 10,000 blocks. Envelope bounds include companions and spires.
Three nearby nearest-center pairs per seed also use a 16-block native solid-column
footprint grid; sampled shores are not exact minimum-distance proofs.

| Seed | Interior anchors | Center Y | Minimum envelope gap | Sampled shore gaps |
| --- | ---: | --- | ---: | --- |
| 1 | 8 | 314–459 | 1644 | 2818–2948 |
| 42 | 9 | 269–490 | 1716 | 3040–3380 |
| 8675309 | 6 | 265–463 | 1716 | 2891–3220 |
| 424242 | 8 | 246–456 | 1655 | 2941–4538 |

All values are blocks. Complete-cluster samples passed floor/headroom bounds.
Typical sampled shore gaps exceed the preferred 1500–2500 range. The conservative
bound is retained rather than claiming tighter spacing is safe from a few seeds.
Biome exclusions can create still longer journeys. Activation remains 0.85.

Generated resource observations use 49 spatial strata per anchor. Counts below
are observed blocks in sampled anchor-owned columns, not whole-island inventory.
The Atlas retains independent estimates, individual strata and artifact provenance.

| Seed / anchor center Y | Observed block Y | Zinc | Diamond | Platinum | Surface-opening columns |
| --- | --- | ---: | ---: | ---: | ---: |
| 42 / 353 | 49–398 | 546 | 142 | 126 | 67 |
| 42 / 465 | 244–529 | 131 | 15 | 8 | 46 |
| 42 / 301 | 25–349 | 184 | 17 | 24 | 0 |
| 8675309 / 400 | 102–449 | 359 | 59 | 59 | 0 |
| 8675309 / 321 | 50–363 | 258 | 18 | 31 | 0 |
| 8675309 / 265 | -34–316 | 279 | 52 | 42 | 0 |

All six anchors also contained iron, copper, coal, redstone and andesite. Logs
were observed on five; sparse sampling does not establish that the sixth lacks trees.
Emerald appeared in one eligible sample; absence elsewhere does not establish a
missing ore. Native rarity and biome restrictions remain unchanged. Resource
scans took roughly 31–40 seconds per three-anchor run, excluding startup; these
are bounded observations, not a throughput benchmark or survival mining budget.

Surface-opening counts require generated air at least eight blocks into predicted
surface rock. They include feature/carver results and do not prove deep cave
connectivity. Shader-client inspection at `(-1983, 397, -2404)` found a surface
opening into stone; `(-2256, 501, 2028)` exposed a deep, open-sided chasm. These
checks do not make every island cave-accessible or establish a safe walking route.

The interior block samples contain cavities at multiple reference depths. The
corrected reference surface places upper origins near ordinary ground while deep
caves remain in the body. No separate shallow-carver pass was added.

## Features and integration

Seed-42 targeted chunks stored lush, dripstone and Deep Dark biomes at projected
depths while preserving sampled surface biomes. Actual decoration included moss,
clay, cave vines, spore blossom, dripstone and sculk. A direct native configured
geode placed at Y=170 produced 603 amethyst blocks and 46 budding amethyst; this
checks the feature path, not natural frequency.

Temperate water/snow predicates remained warm and native cold biomes remained cold.
Ruined portals retained correct cold classification in cherry grove at Y=488 and
snowy plains at Y=505. A direct native iceberg attempt left no packed ice at the
sampled oceanless world floor. No iceberg suppression was added.

Both fossil placements, underground clay and standalone vines were reviewed at
placement-provider and feature-code level. Fossils retain template fitting and
empty-corner rejection. Vines retain attachment checks. Blue ice remains
intentionally unadapted because its water/packed-ice environment is absent.
Natural fossil frequency, ancient-city and stronghold realization, and complete
shallow/deep cave connectivity remain unverified. Sky Archipelago's specialized
underground-structure handling is retained; raw vanilla start-height JSON alone
does not establish its final placement.

The village survey found 21 anchors, with allowances `[18, 1, 2, 0]` for zero
through three. Three natural starts survived layout/biome/clearance checks; two
were fully generated. Bounds followed host surface datums Y=381, 493 and 373.
The original test's fixed Y=400–650 assumption was replaced with the same relative
layout envelope around each host. Reverse-order planning and reopening matched.
The 0–3 policy, native layout and biome rejection remain in force.

Both completed villages passed native delivery surface checks through the scoped
retry. Landings were `(-1922, 417, -1134)` and `(-1522, 506, 2766)`.
The future probe reproduced unloaded requests without tickets, then succeeded
with explicit tickets and the production ADQ lease; rejection released the lease.
This does not prove cargo delivery completion or route affordability on the wider
grid. The pack's destination sampling radius needs to accommodate the new spacing.

The Sable curve is unchanged and continuous in world altitude. Its pressure is
1.3 at Y=180, 1.15 at 400, 1.0 at 508, 0.8 at 600 and zero at 768. Lower islands
therefore have denser air. The client received the expanded envelope and correct
pressure values. The previous release's glued, powered balloon completed an
outward/return cargo crossing and save/reopen check; it is historical evidence,
not a new-grid fuel/time test. No controls, gravity or lift implementation changed.

## Client sky and DH

A disposable full client joined a loopback server using the selected
Complementary Unbound r5.9.3 / Euphoria 1.10.5 shader. Controlled spectator views
at Y=100, −64, −128 and −180 were compared at noon/midnight with shaders on/off.
With shaders enabled the sky remained visible below the floor at both times.
Without shaders the lower sky darkened; it did not revert to bright sky at the
lower sample. An initial uncontrolled fall reached the void death screen and was
excluded from the altitude comparison. No persistent missing-terrain defect was
established, and no cosmetic shader option was enabled as a substitute.

Iris supplies the dimension minimum to `bedrockLevel`; the selected shader's
optional `BEDROCK_NOISE` is disabled. These implementation facts agree with the
observed shader behavior but do not establish every shader/GPU path. Veil Iris Fix
remains independent and unchanged.

The isolated dedicated server completed a 32-chunk-radius DH `CHUNKS_ONLY` pregen
at `(-6210, 6210)` with the client closed, without expired requests. A preceding
run at `(6210, 2080)` stalled, expired four requests and then completed; its cause
was not isolated. Thread inspection showed workers waiting on native chunk
futures, with no host swapping or memory pressure. DH already retains tickets in
this path, so the ADQ fix is not a justified substitute. This transient remains a
limit, not a proven Compat defect. DH initialized LOD rendering in the shader
client; a bounded pregen is not a guarantee that an overnight run cannot fail.

## Maintenance limits

The generation JSON, embedded settings, mapping and climate semantics participate
in saved-world identity. Unit checks cover invalid profiles, unsafe spacing,
integer parsing, mapping boundaries and identity rejection. Never delete a marker
to adopt incompatible chunks. Ordinary Sky Archipelago, Nether and End scope
controls remain; full foreign-world content surveys were not repeated for this pass.

Keep the native probes and owning docs; remove disposable worlds, decompiled
sources, captures and raw logs after incorporating results. Re-run affected native
checks when changing Sky Archipelago shape internals, feature IDs, village layouts,
ADQ private methods or Sable pressure records. Passing the build alone does not
verify those integration contracts.

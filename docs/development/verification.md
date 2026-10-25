# Verification

## Create Sky Compat release verification

Terrain/resource checks use
Minecraft 1.21.1, NeoForge 21.1.264 and the installed Create Sky server mod selection.
Generation checks use isolated worlds rather than modifying existing play saves.

Commands exercised:

```sh
./gradlew check build probeJar
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --terrain --samples 4
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --biomes --seed 42
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --biomes --seed 8675309
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --mechanics --seed 42
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --islands --seed 42 --heap 6G
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --islands --seed 8675309
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --chunk-futures --seed 42 --heap 6G
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --generation adapted --seed 42 --reopen
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --generation current --seed 42 --reopen
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --generation vanilla --seed 42 --reopen
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --natural --natural-profile adapted --seed 42 --reopen --heap 6G
python3 tools/probe.py --server /path/to/server/game --java /path/to/java --natural --natural-profile adapted --seed 8675309 --reopen
```

The native density survey reads the actual registered preset and its biome sampler,
with a 16,000-block descriptor radius and a 10,000-block interior spacing sample.
The marked generator's resolved settings are compared with its embedded settings,
including on a dedicated server with different common TOML values.

| Seed | Interior anchors | Mean nearest distance | Mean sampled height | Mean interior thickness | Minimum sampled headroom |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 | 76 | 1,505 | 343 | 266 | 243 |
| 42 | 74 | 1,526 | 336 | 235 | 233 |
| 8675309 | 67 | 1,532 | 303 | 230 | 247 |
| 424242 | 70 | 1,588 | 331 | 241 | 249 |

Distances and heights are blocks. Four nearby anchors per seed supplied the height,
interior-thickness and headroom samples. Individual sampled heights ranged from
271 to 351 blocks; the target concerns average large-island height. These are
native density predictions before carving/features, not complete block inventories.
Biome filtering retains occasional longer gaps: sampled maxima were 2,199–2,325 blocks.

The generated seed-42 comparison sampled two nine-chunk interiors per preset.
Adapted terrain/query comparisons matched all 4,128,768 inspected positions in
standalone native fills. The custom dimension used Y=-128 through 767. Adapted
ore counts remained identical after saving and reopening.

| Ore | Adapted sample totals | Ordinary Overworld controls |
| --- | --- | --- |
| Zinc | 473, 694 | 677, 687 |
| Diamond | 83, 203 | 245, 228 |
| Platinum | 62, 179 | 239, 250 |
| Coal | 9,117, 9,694 | 792, 730 |
| Iron | 5,110, 5,529 | 697, 724 |

The current Sky Archipelago controls contained only coal and iron in their sampled
interiors. Adapted coal/iron remain abundant because their native upper reference
bands now intersect rock throughout a deep island. Counts have not been reduced;
these are bounded interior samples, not whole-island abundance or survival timings.

A distributed survey sampled three anchors each on seeds 42 and 8675309, using
one random chunk in each of 49 spatial strata per anchor. Only columns owned by
that anchor contributed. All six sampled anchors contained zinc, iron, copper,
coal, redstone, andesite and logs. Combined stone/deepslate zinc counts were
93, 25, 134, 106, 66 and 45 respectively.
These are counts in the sampled columns, not whole-island totals. Diamond and
platinum were not observed in every sparse sample; the independent interior
samples above establish their deeper placements. Native generation and scanning
took about 31 seconds per three-anchor run, excluding startup. End-of-run heap
use was roughly 3.2 to 3.7 GiB, not a measured peak. The latest seed-42 scan used
a 6 GiB heap after a 4 GiB run exhausted memory during shutdown. That failed run
is not a clean runtime pass, even though it wrote counts and exited zero. The
runner now rejects fatal/shutdown errors separately from the report's completion
flag. The 6 GiB repeat completed and stopped normally in 29.4 seconds of scanning.

Exact companion groups contained 7/4/7 and 6/5/2 descriptors across the two seeds.
Sampled center-line void gaps were 973 to 1,046 blocks for seed 42 and 511 to
1,211 for seed 8675309. These use density queries at intervals no greater than
eight blocks. They identify crossing scale, not a flown route or safe altitude.

The initial probe ran in `ServerStartedEvent` and could precede Sky Archipelago's
spawn-fallback listener. A repeat then recorded the temporary vanilla spawn over
void. The maintained probe now waits for the first completed server tick. Two
fresh seed-42 runs subsequently agreed on `(610, 511, -626)`, supported by grass
with collision-free feet/head space. Seed 8675309 selected `(-449, 517, 610)` on
grass. This was a harness-ordering correction; no production spawn patch was added.
Native spawn selection still does not prove safe respawning or every seed.

A direct native configured-geode placement at relative reference Y=0 (world Y=238)
produced 588 amethyst blocks and 45 budding amethyst. It verifies feature geometry
fits deep island rock, not natural geode frequency. Candidate hash remained
`8087bf6bcd4e598d8b14e0383bd49c0d52a840121cf40c9f500e498906f157f0`;
that feature run used probe
`1dde2334830941468e9aca046126679647b9f2a91822e2b9ad53a1c63083239f`.

Before relative carving, the same adapted interiors contained zero and 333 air
blocks where the density fill predicted rock. With carving and geological feature
adapters they contained 11,249 and 14,530, distributed across several reference
bands. Water and lava sources also appeared. The measurement includes any cavities
made by features; it does not attribute every air block to a specific carver.

A staged control omitting only the three carving mixins retained the same cavity
histograms in the ordinary Sky preset. Post-decoration ore/vegetation counts varied
by a few blocks across control runs; this harness does not prove byte-identical
feature generation under the installed C2ME stack. The adapter's deterministic
origin frames and saved/reopened observations are narrower verified properties.

Cave-biome checks used seeds 42 and 8675309, one generated chunk for each of
Deep Dark, lush caves and dripstone caves. Before projection, the seed-42 lush and
dripstone fixtures stored surface biomes internally and contained no corresponding
cave decoration. After projection, both seeds retained all three intended cave
biomes and unchanged surface samples. Dripstone chunks contained 80/82 dripstone
blocks and 20/30 pointed dripstone; lush chunks contained moss, clay and cave vines.
Deep Dark retained its upstream projection and generated sculk. These targeted
positive samples establish working biome/feature paths, not their regional rarity.
The tested cave candidate was
`98b6d32ea27ed503f0d71c49470d98d0c9293eddf22d7ce38e5f365b3999f187`,
with probe `ddcb0ce98e61dd1d7661281611806417f5f72a79b127f35089c2cf04175cfa97`.

The climate fixture reproduced native altitude cooling in birch forest at Y=508.
With the scoped correction, temperate water did not freeze and snow did not form;
snowy-plains predicates retained both behaviors. Nether scope remained unchanged.
Ruined-portal generation separately reproduced incorrect cold classification in a
taiga at Y=412. The scoped portal adapter changed its stored cold property to false;
a snowy-taiga portal at Y=496 retained true. A direct native iceberg placement at
the oceanless preset's build floor returned success but left no packed ice in the
sampled bounds. No iceberg suppression was added from that source-level concern.
Portal checks used candidate
`8087bf6bcd4e598d8b14e0383bd49c0d52a840121cf40c9f500e498906f157f0`
and probe `9e8229e84d61b77b8c2b146f503279ac35c2731a5033e7dbfb865a4f7e1f37b9`.
The client rendered rain near Y=702 and snow at `(4825, 597, 812)` in a
native-located snowy-plains biome. The cold fixture used a temporary stone platform;
it was not a naturally generated landing surface. Snow rendered with and without
Complementary/Euphoria, with rain visible across the nearby biome boundary. These
checks used the final `fe4d658a…` candidate. Distant Horizons initialized its LOD
renderer and terrain remained visible through the shader toggle. This establishes
visible rendering in this lab, not complete LOD accuracy or another GPU's behavior.
Iris logged one opaque/translucent-pass error at initial connection; no persistent
missing terrain was observed. Existing Veil bool-uniform warnings remain outside
the independent framebuffer fix's scope.

The village planner uses the 0–3 logarithmic allowance and checks every selected
site in a 12,000-block descriptor survey, plus 625 random-spread candidate chunks.
With the authoritative embedded settings, seed 42 sampled 111 anchors. Allowances
for counts zero through three were `[81, 19, 7, 4]`; the selected-site histogram
was `[85, 16, 8, 2]`. Native generation retained `[90, 17, 4, 0]`, totaling 25 starts.
Two villages were fully generated. Accepted footprints passed terrain clearance
and remained beside their assigned anchor; native locate found both complete
villages. Fresh reverse-order planning and save/reopen comparisons retained
identical sites and observations.

The final seed-42 pass also counted three/nine diamond ore and 27/26 emerald ore
inside the two village bounds. Both contained all four crop types, bookshelves,
lava and a brewing stand. Their 16/23 realized chest tables returned one/eight
diamonds, 17/10 iron ingots and eight/12 obsidian in deterministic no-player sample
rolls. These are two natural bounding-box samples, not revised foundation rates or
Lootr entitlements. Chests were still vanilla containers at this probe checkpoint;
later runtime conversion and per-player access were not exercised.

ADQ 1.1.1's native surface resolver rejected both locate points, `(96, 0, 816)` and
`(1936, 0, -320)`, beside void. The applied handler found `(97, 512, 814)` and
`(1934, 563, -322)`, respectively. Both pass the original dry, even 3×3 surface
predicate and lie inside the village bounds. Valid native results remained
unchanged; foreign dimensions and unloaded/non-village points received no retry.
The observations, loot rolls and endpoints matched after save/reopen. Actual cargo
placement, a flown approach and quest completion remain separate checks.

This delivery/loot pass and the clean seed-42 island scan used candidate
`94abe7a6a653011ead8bfd621948b5eef966e904c4d56e1858a8419334de625f`.
The delivery/loot probe was
`bb66fae0704c58602245a786eb5943ae22d34f75a128bff368764db7597c2612`;
the island probe was
`2b47b138b07c019d72e60b550b02ce9b646f39370a66d8530b04204240e1e69d`.
Installed ADQ SHA-256:
`7c61641b68e59aa35ac6277784b23c6cc8649ca7f44a916b979e936a87060cc5`.

A later native console `adq generate` exposed a separate background-loading failure.
The dedicated future probe reproduced `Unloaded chunk` for both FULL requests at
chunks `(6, 51)` and `(121, -20)`, despite `create=true`. Explicit retained tickets
made both complete successfully. The production adapter then loaded both endpoint
neighborhoods successfully; invoking ADQ's applied rejection handler released the
lease. This is installed-stack evidence, not a minimal vanilla/C2ME attribution.

With loading repaired, native ADQ generated a one-block route inside the same
village. Its pre-resolution Y=0 versus Y=518 locator positions had passed the
500-block distance test. After using horizontal distances, an actual generated
offer ran from `(2240, 518, -3887)` to `(-775, 524, -4079)`, about 3,021 blocks.
Registration released all 18 owned tickets. The generated offer was accepted through its native table UI. ADQ placed its
82-block heavy secure container at a nearby valid position and tracked pickup;
cancellation removed the cargo body and released its chunk ticket. The cargo was
not transported to completion. A separate Sky Village placement fixture was
interrupted by incorrect player footing; that check remains unfinished. No
delivery-completion or survival-flight claim follows.

The ticket/distance candidate was
`fe4d658ac3ef228a0572df4557f8f6b5b29a94e7402ba6e86cc976d23cd06930`.
The final endpoint/loot reopen pass used probe
`4a5e990747b5472f650b5e78cc425b91446f1af39efde6b8cb07c4b07fce6ebc`.
Both generated villages passed the native landing and horizontal-distance controls;
foreign dimensions retained their native three-dimensional metric. The exact
500-block horizontal boundary returned squared distance 250,000. The test selects
the merged handler by signature, excluding MixinExtras' synthetic `LocalRef` bridge.

The ordinary background control failed in 0.11 seconds, retained control completed
in 1.03 seconds, and the production-helper case completed in 0.50 seconds after
those chunks existed. These timings are diagnostic observations, not comparable
cold-generation benchmarks. Timeout and shutdown-during-load fault injection remain
unperformed; normal server shutdown passed.

The count draw has exact weights `96:24:8:3`; observed percentages in one region
need not match them. Native biome and terrain rejection can reduce actual counts.
Rejected layouts are not retried until a quota is filled. Earlier lower-density
controls across two seeds also exercised a fully realized three-village cluster;
the current seed-42 survey did not retain three villages at any single anchor.

The native Sable fixture loaded the scoped pressure record, verified bounded,
monotonic interpolation at every integer altitude in `[-128, 768]`, and round-tripped
its native network codec. Pressure was 1.0 at Y=508 and zero at Y=768. Nether lookup
remained unchanged. Ore counts were stable when that sample was reopened. This
validates data and routing, not aircraft flight or live client synchronization.

A separate offline lab client connected to an isolated loopback server using the
new preset. Client telemetry read the custom physics record and the expected
pressure values at all eight control heights, with build bounds `[-128, 768)`.
The installed Complementary/Euphoria shader stack rendered terrain and the moving
craft. This is a visual compatibility observation, not a performance benchmark.

The native balloon fixture used a 5×5 plank platform, a powered adjustable burner,
a chest with 64 iron ingots, a physics assembler and a 7×7×7 hollow wool envelope.
The envelope's bottom had a 5×5 opening; four fence supports connected it to the
platform. A Create super-glue volume covered the complete fixture. Verify its
saved `Pos` plus relative `From`/`To` bounds before assembly: summoning the entity
at a corner re-centered its box. The earlier unsupported envelope split from
its platform, so that run is not evidence of loaded flight.

Dragging the native assembler lever assembled the connected fixture as one tracked
body. With the player aboard and burner setting 500, it rose from a platform at
Y=550 to roughly Y=703 and settled below the ceiling. After `save-all flush`, normal
server shutdown and restart, the same sub-level UUID and force-loading ticket
returned. The chest retained all 64 iron ingots and the burner retained its setting.
Client observations again showed one body hovering near Y=703. This establishes
one balloon design's ascent, settling and persistence; other ship designs and
survival routes remain separate checks.

The burner setting is gas output in cubic metres, not temperature. The goggles
reported a 146 m³ envelope. Setting 150 therefore still filled it; reducing the
limit to 60 caused a controlled descent and settling near Y=604. Restoring 500
caused ascent again. These output changes used native block-entity data commands;
the assembler lever, boarding and rendering were exercised through the client.

The client used candidate
`8087bf6bcd4e598d8b14e0383bd49c0d52a840121cf40c9f500e498906f157f0`,
Veil Iris Fix `31c784adc24fa2e8d83b5bcebf0aed4e61d9f0be568d630ac1e28767ef34487f`,
and client probe `8b52347acb08e584485183aa6a058f8355513d0621b070923dcfc5ea8c63e177`.

The installed Aeronautics 1.3.2 balloon search rejected open columns beginning at
Y=-127, -96, 508, 760 and 767. At the first four heights it also detected all 27
air blocks in an enclosed wool cavity. Its build-height guard terminated searches
at the expanded ceiling. This did not reproduce the older-version stack overflow
in [Simulated issue 1367](https://github.com/Creators-of-Aeronautics/Simulated-Project/issues/1367);
no workaround was added. It does not establish safety at arbitrary taller envelopes.

Simulated's native assembly/disassembly helpers moved and restored a slime block
and chest at Y=-127, -96, 508 and 766, preserving seven diamonds in the chest.
The highest chest occupied Y=767. The fixture allows 40 normal ticks before shutdown
so deferred empty-body cleanup can finish; stopping directly in the startup callback
produced a removed-body save error. The corrected run exited normally. These are
block-transfer checks, not loaded aircraft flight or saved-ship persistence tests.
The mechanics check used candidate
`8087bf6bcd4e598d8b14e0383bd49c0d52a840121cf40c9f500e498906f157f0`
and probe `bcd2cd111e5f030daedb4f6756a0ee838d2550336710f08ceb9fba30bfd9e2df`.

The extended mechanics fixture also calls Sable's installed sail lift/drag method
on real assembled sub-levels at Y=-127, -96, 508 and 766. With identical controlled
velocity inputs, native lift strength fell from 7.127 to 7.108, 4.752 and 0.0024;
all force/torque components were finite. Stationary controls returned zero lift
and drag. These values are native method outputs for one orientation, not measured
wing-borne flight performance. The altitude sensor read each block center's world
Y, round-tripped normalization across the expanded envelope, and returned the
correct scoped pressure. No sensor or aerodynamic patch was needed.

Tested artifact SHA-256 (probe for the following surveys:
`5ffa82c36a91f1c5eab9d4bcbaf129ccb627fc437877b807c9d71e1b385909cf`):

- Actual-preset geography and pre-carving control:
  `a47a2ac51aa594584f419eac3a255870f43bfc9c39bd5ab6c60cd5a463c958af`.
- Relative carving/features, generated ores, villages, climate and pressure:
  `e66be97273cfc610147979289df158da6db6c0c6b9189a6946bfae222fc2f97f`.
- Staged carving-disabled diagnostic control:
  `3c8b4f3cdd2bb2ca0216d4fb2624cafbb48fd229305e835ec4f372cfdca82eb3`.

A wooden propeller driven by a creative motor moved the same loaded craft at
roughly Y=702. At +128 RPM it travelled toward decreasing Z; changing to -128 RPM
reversed travel past the starting Z. Its native rotation telemetry changed from
+38.4 to -38.4. With the motor set to zero, native speed became zero and movement
slowed: Z changed from 185.70 to 188.18 in seven seconds. This is a powered
reversal/drag observation, not an island-to-island return journey or a survival
fuel-cost test. The chest still contained 64 iron ingots at shutdown. Native rain
rendered at this altitude with Complementary/Euphoria enabled. The lab server and
client saved and stopped normally.

The final combined client/server candidate also completed a powered cargo crossing
between the seed-42 anchors around `(820, -673)` and `(2453, -932)`. The same
connected body travelled from `(820, 605, -673)` to a hover at approximately
`(2321, 593, -966)`, then returned to `(819, 661, -619)` before stopping. Both legs
used native propeller forces; no body teleport or force injection occurred during
the crossing. The chest retained 64 iron ingots. Outbound movement took roughly
five minutes at about five blocks per second; the interrupted return is not a
comparable travel-time measurement.

The single low propeller pitched the craft. The crossing fixture instead used two
wooden propellers on opposite envelope sides, near its center of mass, powered by
creative motors. Small RPM differences steered it. Console commands adjusted
motor and gas settings, so this does not verify a complete survival control/power
arrangement. The player rode outbound and most of the return; the return paused
for reboarding after a failed boarding teleport following the separate weather
check. Player and cargo were aboard at the final return observation. Gas output
was 60 outbound and 75 returning; powered pitch altered the equilibrium altitude.
The route demonstrates transport and return capability for this fixture, not
survival costs, docking, autonomous flight or arbitrary aircraft stability.

Wing-borne aircraft, survival mining/travel timings and broader resource/biome
coverage remain unverified. Native sail-force checks establish the installed lift
and drag response separately. The listed geography, resource and village results
use the corrected preset configuration precedence.

## Build and package boundaries

`./gradlew check build probeJar` passed with 26 JUnit checks, five Python release-tool
checks, three pinned upstream hashes and package/dependency checks. These cover
geometry, count weights and boundaries, order-independent site selection, generation
identity, climate scope, release staging and absence of bundled upstream classes.
They do not establish gameplay balance or native client rendering.

## Neighbor-rule baseline

The previously released neighbor filter exercised 48 native layouts across seeds
1, 42, 8675309 and 424242. Of these, 42 passed upstream SKY clearance. Retained
layouts at maximum gaps of 384, 512 and 768 blocks were 5, 14 and 16 respectively;
the 512-block default preserved more nearby candidates without the longer reach.
The filter did not move layouts and left unrelated SKY structure IDs unchanged.

Those measurements support the horizontal defaults, not the new preset's density.
Its wider, deeper terrain and count policy require their own native samples above.
The diagnostic `--disable-carver-adapter` option removes only those three mixin
entries from the staged copy of the mod. Reports hash those actual staged bytes;
it never modifies the built artifact or a playable installation.

No live save was opened, regenerated or synchronized. Probe worlds are disposable
and removed by the runner after each run. Client rendering, loot interactions and
progression remain separate verification work.

## Optional integration loading

The optional-loading change passed `./gradlew --no-daemon check build probeJar`.
Both native runs used Java 21 and the existing runner against the installed
server's read-only libraries: `--generation adapted --seed 42 --reopen --heap 6G`,
with `--minimal` for the minimum installation and without it for the full pack.
Each sampled 18 interior chunks, verified 4,128,768 density/base-column positions,
checked scoped climate and retained the sampled ore counts after reopening.
The minimal runtime contained only Sky Archipelago 1.3.6, Compat and the separate
probe. All optional integrations reported absent. The full runtime reported all
six integrations present and passed Sable pressure interpolation/network-codec
checks. Intermediate dependency combinations were deliberately not run.

Tested pre-release-metadata artifact SHA-256:
`981ae7bbea0262594fb12989a7b20617fff1a1164b9b088a5a1fde2e1bd3be72`;
probe `f906de48435ad840d428a5181189ce6844f32f8c3aae8d8d702d4c03217140f7`.
Subsequent release changes only rename repository URLs and set version 0.2.0;
production classes and resources otherwise retain the tested behavior.

The final 0.2.0 minimum client joined a dedicated minimum server, spawned on solid
terrain at Y=511 and rendered the island with no optional mods installed. The
full client loaded the refreshed Resource Review world with the selected shader
stack. These are startup, networking and rendering checks, not performance tests.
The client-only weather mixin and configuration entry point are isolated from
server initialization. Sable/ADQ mixins are selected from early loader metadata;
village hooks require both Sky Villages and Architectury. Veil Iris Fix remains
an independent client-only artifact, excluded from server pack payloads.

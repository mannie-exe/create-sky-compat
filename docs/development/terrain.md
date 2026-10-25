# Island generation

The `create_sky_compat:islands_v1` preset owns the expanded terrain described here.
The [verification record](verification.md) distinguishes native observations from
unverified survival balance and vehicle capabilities.

## Ownership and scope

The preset owns its dimension type, noise settings and embedded Sky Archipelago
settings. Both height envelopes are `[-128, 768)`. The noise settings retain
Minecraft 1.21.1's Overworld router and surface definitions with the expanded
envelope. Sky Archipelago still supplies terrain filling and surface treatment;
this does not restore vanilla aquifers or the separate large noise-vein system.

`GeneratorProfileMixin` opts in only when the generator references the versioned
noise-settings key. It configures each field before returning it to a caller.
The field, sampler and descriptor factory share an immutable `GenerationProfile`;
there is no global current-world flag. Other presets retain upstream terrain.

The marked generator resolves its embedded settings on both dedicated servers and
singleplayer. Upstream otherwise gives a dedicated server's common TOML priority,
which would make the same preset produce different settings across runtimes.
The adapter preserves upstream resolution for every other generator. Missing
embedded settings fail explicitly rather than falling back to a host's defaults.

`IslandGeometry` retains upstream archetypes, horizontal shape noise and shape
parameters while separating anchor dimensions from companion radius/orbit ranges.
The spatial grid stays fixed. Its jitter changes center distances without breaking
floor-division lookup or bounded neighboring-cell searches. Terrain fills, height
queries, structure support and previews all consume the same descriptors.

Main-island radii remain 270 to 370 blocks; companions retain radii 40 to 110
and orbits 470 to 680. Spires retain their native smaller dimensions. Each cluster
selects a uniform integer target height from 250 to 400 using its seed and a
separate salt. The anchor's cliff depth is target height minus plateau height and
32 blocks of relief/underside allowance. This is a target for the whole island,
not every column or a promise of exact realized height. Native cutouts, relief
and erosion still affect the measured extrema.

Placement uses each selected depth. Requested centers remain Y=200 to 490, but
tall bodies need a higher lower limit: at defaults a 400-block target requires
Y>=268, while a 250-block target can start at Y=200. The complete shape reserves
32 blocks above the floor and 200 above terrain. Companion elevations are fitted
independently. Terrain and geology are not compressed to fit.

`IslandDensityMixin` configures only fields belonging to this preset. It prepares
lower-body and horizontal bounds once per native column contribution, storing
them on the prepared density object. No global coordinate cache or extra noise
sampling per voxel is needed. The pinned upstream anchor rule subtracts a fixed
0.55 plus a depth term below `underside - 4`; the adapter removes that jump and
intersects the remaining native density with a continuous curved lower boundary.
An elliptical quadratic term and bounded eight-block noise vary the underside.
The existing native erosion and archetype coverage remain active. The taper stays
inside the selected body rather than extending a long tail beneath it.

All island families also have continuous radial support ending at
`2 * maxRadius + 32`. This is an explicit shape bound, not an inference from a
few sampled shores. It leaves nominal radii and companion orbits unchanged but
can remove an unusually distant native fringe. The maximum complete-cluster
reach is 933 blocks, including rounding of companion offsets. Spacing 2400 with
jitter 90 leaves at least `2400 - 180 - 2 * 933 = 354` blocks between density
supports, satisfying the configured 350-block minimum. Trees, structures and
player construction are
outside that terrain-density guarantee. Actual shore distances are measured
separately, and missing clusters can create longer journeys.

The density mixins depend on Sky Archipelago 1.3.6's `prepareDensity`,
`samplePreparedDensity`, prepared-object lifetime, and underside penalty formula.
An upstream update requires inspecting those methods, reproducing the native
cutoff control, and rerunning bounds, silhouette, chunk/query and reopening checks.
Matching signatures alone is insufficient. Ordinary presets retain native density.

The native fresh-world fallback predicts a landing before carving/features have
necessarily left the same surface. `SpawnFallbackMixin` validates only its final
spawn assignment in this preset. `SpawnLanding` checks the generated chunk's
motion-blocking height, solid support, clear feet/head and fluids, then searches
bounded four-block offsets up to 16 blocks if necessary. It fails explicitly when
no landing exists. Chunk loads occur only during startup, never in the terrain
query path. Review the upstream fallback's event and saved-world guard when
updating; this hook must not relocate existing worlds.


Embedded settings retain biome eligibility, archetypes and structure policy.
Their serialized spacing is a placeholder: upstream caps that field at 2000,
while the field and sampler adapters own the real grid. Upstream common TOML,
DYNAMIC spacing and size/band controls do not override this preset's geometry.
See [configuration](../configuration.md) for authoritative controls.

## Geological frame

`GeologicalFrame` maps reference Y=−64 to the anchor's `centerY - cliffDepth`
and reference Y=64 to `centerY + plateauHeight + 1`. Underground heights scale between
those points with integer floor division. Heights above reference 64 retain a
one-block-to-one-block offset above that surface datum. Vanilla's build ceiling
is not treated as ordinary ground level. Samples outside `[-64, 320)` are rejected,
not clamped. The inverse uses the same piecewise model for cave biomes and ore
reporting.

The frame is shared across an anchor, its shorelines and companions; a shallow
companion does not acquire a complete compressed ore column. The datum is not the
actual surface of every column. Relief and erosion remain meaningful: upper iron,
coal and emerald attempts can land in air, while attempts below the datum remain
in the deep body where replacement rules permit them. Native counts are unchanged.

`OreHeightMixin` intercepts the existing height modifier only for features in
`#create_sky_compat:relative_geology`, inside the opted-in generator. It samples
the original height provider against an explicit reference context. Absolute,
bottom-relative and top-relative anchors retain their original meaning. It
consumes the sample even when the target column is void, then maps successful
samples. Counts, feature order, biome checks, configured veins, replacement rules
and exposure probabilities remain native. Nether/End and unlisted placements
are unchanged. Feature-internal checks remain native. Fossils still fit their selected template
below the local heightmap and reject excessive empty corners. Standalone jungle
vines use a separate `relative_surface` tag: reference 64 to 100 becomes actual
column top through top+36, preserving their neighbor-attachment test. Blue ice is
not moved: its native feature requires water, packed ice and a position below sea
level, which this oceanless preset does not provide naturally. Moving its origin
would not supply that environment. Ocean-specific content is not guaranteed.

The tag includes vanilla Overworld ores, underground clay, both fossil placements,
geological patches, Create zinc and
Overworld striated deposits, and Propulsion platinum. Mod entries are optional
registry references; their absence does not invalidate the datapack.

Geodes, Overworld water/lava springs and underground lava lakes use the same
height-placement adapter. Native rarity/count modifiers, environmental scans,
surface-distance checks and feature geometry remain intact. Surface-heightmap
vegetation is not moved through the ore frame. Cave vegetation, dripstone, sculk,
underground monster rooms, glow lichen and underwater magma use their native
height providers through the same adapter. Environmental checks remain native.

Material plans map their deepslate test through the same frame. Terrain fill and
base-column queries use the same adapter, retaining the native transition noise.
The preset's deepslate transition is expressed at reference Y=0. Frame lookup uses
cached terrain descriptors and bounded cell searches; it never loads chunks.

## Carving

Vanilla cave origins sample the reference envelope and map through the geological
frame at their source chunk's center. Canyon origins instead preserve their native
10 to 67 height spread relative to the actual local surface at reference Y=64. That fixed origin
frame is shared by every target chunk carved by the same tunnel. Tunnel lengths,
branching and shapes remain native. A void origin consumes the native random
sample and subsequent tunnel decisions below the playable envelope; it cannot
manufacture a new geological frame at an island edge.

The lava-floor anchor resolves through the frame at each carved block. The real
carving context retains the full dimension bounds; canyon width arrays and native
clipping therefore still use valid world coordinates. No neighboring chunks are
loaded for these lookups. Other generators retain their original provider calls.
This restores classic carvers within island rock; it does not introduce vanilla's
noise-cave or aquifer terrain-filling systems.

## Cave biomes

During native chunk biome filling, lush and dripstone cave footprints are sampled
at island-relative reference depth. Projection requires an existing solid column
and leaves its upper 16 blocks to the original surface biome. It shares the anchor
geological frame with ore placement; thin companions do not gain an independent
complete cave-biome column.

The adapter extends Sky Archipelago's existing projection scope and recursion
protection. Its Deep Dark projection retains priority and upstream behavior.
Direct biome-source calls used by structure eligibility remain unchanged; adding
structures that require lush or dripstone biomes needs a separate placement review.
Vanilla cave decoration retains its own ground, ceiling, water and space checks.

## Climate and villages

Temperature comparisons use reference Y=64 throughout this preset. Climate follows
biome rather than absolute altitude: a high temperate island does not become snowy
solely because it is high. This also keeps multiplayer client weather and server
freezing consistent without reconstructing server terrain or exposing its seed.
Altitude cooling above a mountain is deliberately absent in this preset. The
adapter changes only temperature-query positions; light, fluid, terrain and block
coordinates retain their real altitude. It covers Sky Archipelago surface
selection, ruined-portal cold classification, vanilla snow/freezing predicates,
precipitation ticks, rain exposure
and the client's weather rendering/sounds. Native snow rendering was checked
with shaders enabled and disabled. Surface selectors receive a bounded thread-local context from
the owning surface pass because their API has no world argument; `finally`
restores it across nested calls and failures. No biome temperature is mutated.

Sky Villages keep their authored start-height spread, shifted from the old
Y=192 datum to the new anchor surface datum. Only the named structure and the
opted-in generator receive this adjustment. Horizontal clearance still uses the
complete generated footprint.

Each archipelago has an allowance of zero to three villages, with zero common
and three rare. The logarithmic-series weights are
`96:24:8:3`, or approximately 73.3%, 18.3%, 6.1% and 2.3%. `VillageDistribution`
implements the deterministic count draw. `ArchipelagoVillagePlacement` replaces
only the named village's runtime placement inside the opted-in generator. It
selects stable random-spread candidates per anchor; it never uses mutable accepted
counters or chunk visitation order. Companion islands do not receive separate
quotas. Layout, biome and clearance rejection can reduce the realized count.

The candidate grid uses 24-chunk spacing and 12-chunk separation. A candidate
belongs to the nearest anchor, with coordinate tie-breaking. It must pass a cheap
center-biome eligibility check before selection. Seeded ranking selects at most
the anchor's allowance, leaving at least 384 blocks between its candidate centers.
Native Jigsaw still checks the actual start-piece center biome and native layouts
still pass complete-footprint clearance. The preliminary ring reserves 128 blocks
for layout extent; it does not replace the final terrain check. Original salt,
spread type, frequency, exclusion-zone checks and locate behavior are retained.
Registered structure sets and ordinary Sky Archipelago worlds remain unchanged.

Candidate admission also reserves a 192-block square inside the anchor's nearest-
center region: 32 blocks for upstream SKY biome retries and 160 for layout extent.
The final layout must retain the same nearest anchor as its native start. This
prevents a village assigned to one cluster from consuming space beside another
cluster with a different allowance. Review these bounds when changing Sky Villages'
start pool or Jigsaw radius, or Sky Archipelago's source-retry search.

## Delivery endpoints

When Aeronautics Delivery Quests 1.1.1 is installed, the adapter changes its
structure-route loading, distance checks and surface finalization only in the
opted-in preset. RANDOM routing and foreign generators retain native behavior.

The background FULL chunk requests need retained tickets in the installed stack.
Without them, native futures can complete with an `Unloaded chunk` result that
ADQ ignores. `DeliveryChunks` holds a 3×3 chunk neighborhood for each endpoint,
at most 18 unique chunks per generation job, and waits for successful FULL results.
Loading remains asynchronous, with a 60-second timeout. Tickets are released on
registration, rejection, load failure or server shutdown. No chunk ticket is
stored in the world or retained for the accepted quest's lifetime. ADQ's single
active generation job is part of this version-coupled contract.
Native generation may request additional neighboring chunks at intermediate
statuses; 18 is the owned ticket bound, not a total generated-chunk budget.

The origin and route minimum-distance checks use horizontal distance. Native
locators can report Y=0 before realization and the actual surface Y afterward;
counting that difference admitted a one-block route as more than 500 blocks long.
The configured maximum still bounds the sampled search offset, not the final
located route. Surface retries can move the selected positions by a few blocks.

Native valid surface endpoints are returned unchanged. If an endpoint is rejected in the opted-in
preset and its loaded chunk contains a Sky Village start, the adapter searches
at most eight blocks around it, in deterministic rings. Every retry uses ADQ's
original dry-surface test and must remain inside the village's bounding box.
Void, fluids, leaves and uneven 3×3 surfaces remain rejected by that native test.

The existing locator can return a start chunk corner beside void even when the
village has valid nearby ground. This adapter changes endpoint selection, not
structure placement, cargo, rewards or RANDOM-mode routing. Search is bounded
and occurs during quest finalization, not terrain generation. Native surface
queries stay within the retained endpoint neighborhoods; this is not a landing planner.
If no valid nearby surface exists, ADQ retains its failure behavior.

The dependency is optional and version-guarded. Review the private methods before
changing ADQ: each of the surface, chunk-request and distance wrappers requires
exactly two call sites. Registration and failure entry points own lease cleanup.
Do not remove surface checks or expand the search into unrelated terrain to make
a route succeed. A generated vanilla-village offer was accepted and its cargo spawned natively.
Sky Village cargo placement and delivery completion remain separate play checks.

## Atmosphere

The preset uses a Sable dimension-physics data record under
`data/create_sky_compat/dimension_physics/islands_v1.json`. A narrow lookup adapter
selects that profile only for this dimension type. Sable's native loader and
network packet carry the record; no custom packet or global Overworld override is
needed. A higher-priority explicit dimension record remains authoritative.

The pressure curve starts at 1.5 below the islands, reaches 1.0 at the Y=508 pressure
reference, 0.8 at Y=600, 0.5 at Y=680 and zero at the exclusive ceiling of Y=768.
Its cubic interpolation is checked across every integer altitude for bounded,
monotonic pressure. Gravity, drag and magnetic north retain Sable's defaults.
Native aircraft behavior still requires separate flight checks; successful data
loading or interpolation alone cannot validate a ship design.

## Saved worlds

Before levels load, `GenerationGuard` records the seed, algorithm revision,
geometry parameters, resolved embedded settings, village policy and structure set,
biome source, noise settings and dimension
in `create-sky-generation.json` within a new world's directory. Object keys are
canonicalized so map iteration cannot change the identity. Reopening with a
different identity fails before generation. Existing region files without an
identity are not adopted. Replacing the generator in a marked world is rejected.

There is no conversion or automatic marker rewrite. Restore a world's original
settings/mod version or create a separate world. Deleting the marker is not a
migration procedure. Changes to the generation algorithm require reviewing its
revision and compatibility contract before release.

## Native checks

`tools/probe.py --terrain --samples 4` compares actual registered-preset density
samples over four layout seeds. These are predictions before carving/features, with sampled
volume estimates rather than full island block counts.

`--generation current|adapted|vanilla --seed N` generates two bounded interior
samples of nine chunks each using the installed server's mod selection. It records
actual blocks and ore counts by reference depth. For Sky Archipelago generators,
it also fills standalone native ProtoChunks and compares every block with base-
column queries. Those isolated chunks cannot race C2ME surface/feature workers.
Full generated chunks are inspected separately because surface and ore features
legitimately replace predicted stone and deepslate.

`--islands --seed N` samples one deterministic random chunk from each cell of a
7×7 grid inside twice each of three nearby anchor radii. Counts include only
columns dominated by that anchor. Weighting each chunk by its stratum's chunk
count yields a bounded-area estimate, not an exact island inventory. Reports keep
observed counts and estimates separate, plus reference-depth ore bins, surface
biomes, headroom, spawn support/clearance, descriptors, timing and end-of-run heap.
Native cell membership also exports exact companion groups and geological frames.
An eight-block-or-finer center-line density sample records its longest void gap.
Descriptor edge gaps and sampled straight routes are approximations, not navigated
or altitude-safe crossings.

`--biomes --seed N` locates native cave-biome footprints and checks stored chunk
biomes, surface preservation and actual cave decoration in generated chunks.

`--reopen` repeats the generated sample after saving and restarting the same
throwaway world, requiring unchanged ore counts or natural village observations.
The village fixture checks all selected sites in a 12,000-block descriptor survey,
native locate, ownership, complete footprints, fresh reverse-order planning and two
fully generated villages. Successful reports identify the
exact mod/probe JAR hashes. Reports and logs live under ignored `build/verification`.
The runner removes its temporary world. Never pass a live save as a test target.

Before changing upstream versions, inspect generator caching, descriptor records,
column/material planning and placement modifier call paths. Rerun native Mixin,
foreign-preset, bounds, query agreement and reopening checks, then the full resource
and gameplay surveys. Build success alone does not validate an injection target.

API references: [NeoForge 1.21.1 biome modifiers](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/)
and [MixinExtras WrapOperation](https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation).
Biome modifiers alone cannot provide the required per-generator placement scope.

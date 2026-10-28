# Configuration

`config/create_sky_compat-common.toml` is created on first launch. Modpacks
can ship this file in their common overrides. NeoForge's built-in client config
screen edits the same settings; no configuration library is required.

On the first start after replacing Sky Village Neighbors, an existing
`sky_village_neighbors-common.toml` is copied to the new filename only if that
filename does not already exist. The old file is preserved. Do not install both
mods: the new metadata explicitly rejects the old implementation.

| Setting | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Filter new Sky Villages layouts |
| `minimumGap` | `64` | Minimum horizontal column distance to any island terrain |
| `maximumGap` | `512` | Maximum horizontal column distance to a sampled host island |
| `minimumIslandRadius` | `96` | Minimum native descriptor radius of the host island |

Distances and radius accept integers from 0 to 2048. `maximumGap` must be at least
`minimumGap`; an inverted range fails server startup with a configuration error.
Even with a zero minimum gap, terrain inside the village footprint is rejected.
An island's descriptor radius is not a measurement of its habitable surface area.

Settings are captured when a server starts, including a singleplayer server.
Changes require restarting it. Existing generated structures are not removed or
moved. The versioned Create Sky preset records its generation configuration and
rejects changes when reopening a world. Use a disposable new world when comparing
configurations. The recorded identity includes structure eligibility as well as
geometry; updates that change that policy can also require a new world. Ordinary
Sky Archipelago worlds retain upstream behavior, where
changes affect future starts only.

Gameplay integrations activate at startup from installed mods, not from these
placement controls. Village integration needs both Sky Villages and Architectury.
`enabled` does not toggle the terrain preset, Sable or ADQ adaptations. Changing
village integration changes saved generation identity; optional installation is
not permission to add/remove generation content from an existing marked world.

## Island geometry

`config/create_sky_compat-generation.json` is created at startup and applies only
to **Create Sky Islands**. Edit it with the game/server stopped; restart the whole
process to reload it. It is separate from the NeoForge village settings screen.
Use matching files on client and server. Geometry changes require a fresh world;
the saved identity rejects changes to an existing world.

| Fields | Defaults | Purpose |
| --- | --- | --- |
| `spacing`, `jitter` | 2400, 750 | Cell spacing and bounded center displacement |
| `activation` | 0.85 | Native cluster activation before biome rejection |
| `centerMinY`, `centerMaxY` | 200, 490 | Requested anchor-center band |
| `anchorMinRadius`, `anchorMaxRadius` | 270, 370 | Anchor descriptor radii |
| `anchorPlateau` | 28 | Native upper-body shape |
| `anchorMinHeight`, `anchorMaxHeight` | 250, 400 | Uniform target total terrain height, before native relief and erosion |
| `undersideTaper` | 80 | Curvature scale of the lower body; included in target thickness |
| `companionMinRadius`, `companionMaxRadius` | 40, 110 | Companion radii; spires retain 12–28 |
| `companionMinOrbit`, `companionMaxOrbit` | 470, 680 | Horizontal orbit around host |
| `companionMinOffsetY`, `companionMaxOffsetY` | −60, 50 | Companion-center altitude relative to host |
| `minimumClusterGap` | 350 | Minimum separation between different clusters’ individual island support disks |
| `floorClearance`, `buildingHeadroom` | 32, 200 | Terrain clearance inside build bounds |

The loader rejects unknown/missing fields, nonintegral integer settings, impossible
vertical geometry, jitter reaching half the cell spacing, and profiles requiring
more than eight neighboring cells for conflict checks. It does not silently repair
input. Close candidate clusters are rejected by their complete island support
bounds; lowering spacing or increasing the gap can therefore reduce realized
density rather than making islands smaller. Older profiles containing `anchorCliff` need the new height and taper
fields before starting a fresh world; changing the JSON does not migrate a save. The requested center band is intersected with safe shape placement:
each anchor derives its lower placement limit from its selected thickness. With
defaults, short anchors can start at Y=200; the tallest require at least Y=268.
The upper center limit remains Y=490, with independently fitted companions. Relief
scale must remain between zero and one for the verified shape envelope.

Geometry, activation, floor/headroom and gap controls have this single owner.
Upstream size presets, altitude bands, DYNAMIC/CONSISTENT spacing and common TOML
do not control this preset. Its embedded settings still own biome, archetype and
structure policy. The embedded spacing value is retained only because Sky
Archipelago's serialized type cannot represent the actual grid above 2000 blocks.
Ordinary Sky Archipelago worlds continue to use upstream controls.

## Island interiors

`config/create_sky_compat-interiors.json` controls interior selection in Create Sky
Islands. It is generated at startup, applies only to the opted-in preset, and is
part of saved generation identity. Restart with a fresh world after changing it.
All fields are required; invalid values and unknown fields fail explicitly.

| Fields | Defaults | Purpose |
| --- | --- | --- |
| `noiseChance`, `hollowChance` | 0.55, 0.18 | Descriptor selection probabilities; remaining islands keep baseline interiors |
| `shellThickness` | 12 | Vertical rock margin for ordinary cavities; selected openings bypass it |
| `noiseHorizontalScale`, `noiseVerticalScale` | 64, 32 | Passage/chamber noise scales in blocks |
| `passageWidth`, `chamberThreshold` | 0.075, 0.48 | Intersecting-band width and chamber-noise threshold |
| `hollowRadius`, `hollowHeight` | 0.58, 0.24 | Hollow radii relative to descriptor horizontal radius and cliff depth |
| `openingChance` | 0.35 | Selected interiors with an entrance and upper opening |
| `nativeCarvers` | true | Retain Minecraft's subsequent classic cave/canyon pass |

Small bodies without four shell-thicknesses of cliff depth keep baseline interiors.
Set both selection probabilities to zero for the uncaved comparison. This does not
remove upstream density cutouts or native classic carvers. Disable `nativeCarvers`
separately when comparing cavity stages. Interiors preserve exterior archetypes and
taper; they do not alter the anchor-relative ore datum. Large iron/copper veins use
the preset's native `ore_veins_enabled` setting independently of cavity selection.

## Local water

`config/create_sky_compat-water.json` controls local water regions in Create Sky
Islands. It is generated at startup and included in saved generation identity.
All fields are required; changes require a fresh world. This does not enable
Sky Archipelago's global ocean or change the world's sea level.

| Fields | Defaults | Purpose |
| --- | --- | --- |
| `chance` | 0.22 | Anchor candidates considered for a basin; terrain checks can reject them |
| `minRadius`, `maxRadius` | 20, 36 | Basin radius in blocks |
| `minDepth`, `maxDepth` | 6, 12 | Maximum bowl depth below its local water level |
| `overflowChance`, `outletChance` | 0.25, 0.20 | Candidate mode probabilities; the remainder use enclosed basins |
| `maxOutlets` | 1 | Maximum channels or underside openings per open basin |
| `outletWidth` | 2 | Channel/shaft width control in blocks |
| `liningDepth` | 3 | Rock retained beneath the basin and overflow channel |

Set `chance` to zero to disable these regions independently of cavities and native
springs. Admission checks the existing exterior for a supported floor and rim;
the candidate probability is not the fraction of islands with a generated lake.
Open modes deliberately cut channels or shafts through their owning island.
Native fluid ticks produce the falls, and cold biomes can freeze the surface.
Enclosed regions preserve their floor/rim through the cavity and classic-carver
passes. Later structures, features and player edits can still alter the blocks.
Native surface-water-animal spawning uses the basin's level within its planned
water volume. Actual water and the biome's spawn list are still required; the
setting does not make every basin an ocean or populate it with all aquatic mobs.

## Ocean experiments

`config/create_sky_compat-oceans.json` is generated with `enabled: false`. It controls
experimental local ocean habitats in the opted-in preset, independently of the
global ocean layer. All fields are required and included in saved generation identity.
Use a fresh test world when enabling or changing these settings.

| Fields | Defaults | Purpose |
| --- | --- | --- |
| `enabled` | false | Enable ocean-region selection |
| `basinChance`, `coastalChance` | 0.04, 0.04 | Mutually exclusive anchor candidates for an enclosed ocean basin or an outlet basin with shoreline habitat |
| `perimeterChance` | 0.01 | Eligible small companions considered for a water-covered cap with open edges |
| `minRadius`, `maxRadius` | 40, 72 | Anchor-basin radius in blocks |
| `minDepth`, `maxDepth` | 10, 18 | Basin or cap water depth |
| `perimeterMaxRadius` | 64 | Largest companion descriptor radius eligible for a perimeter cap; its conservative footprint can be wider |
| `beachWidth` | 6 | Shoreline-biome band outside coastal basins, where surface height permits it |

Anchor basins must pass terrain-support checks. Their water level follows the
lowest sampled part of the prospective basin, with a limit on the cut below the
center. Perimeter caps instead remove the upper part of a small companion and let
water flow from its existing edges; they do not create a new global water plane.
Selected ocean candidates replace that descriptor's freshwater selection.

Ocean cells follow local water and the original biome's temperature category;
coastal land can receive a beach biome. Sand/gravel beds and native biome features
supply the habitat substrate. This does not enable monuments or guarantee their
resources, natural spawn populations, or a complete ocean progression route.
Keep the experiments disabled outside test worlds until their visual and runtime
results are suitable for the intended pack. [Verification](development/verification.md)
records the tested scope.

## Biome materials

The `create_sky_compat:surface_mud` biome tag selects mud for the upper three soil
blocks where upstream selected grass/dirt. It contains `minecraft:mangrove_swamp`
by default. Existing Sky Archipelago surface-override tags take precedence;
openings, fluids and deeper rock are preserved. Override the tag in a datapack
to change eligibility; the resolved set is recorded in generation identity.

## Interaction with Sky Archipelago

Keep `customStructureRulesEnabled` enabled and retain the SKY category for
`skyvillages:skyvillage`. Disabling custom rules bypasses the upstream validator
this mod extends. Changing the village to a ground category also bypasses this
SKY-specific rule and can change its placement behavior.

Leave the village out of `skyStructureWhitelist`: upstream whitelisting skips
native vertical-clearance validation. It does not disable this mod's horizontal
proximity rule. `DEFAULT` alone does not avoid SKY routing while the ID still
matches a sky category token.

The neighbor filter uses the resolved terrain settings and island field. In ordinary
Sky Archipelago worlds it works with upstream spacing modes without changing them.
Create Sky Islands uses the geometry profile above. Neither path overrides biome
eligibility.

## Tuning

Increase `maximumGap` to admit more distant floating villages. Decrease it to keep
villages closer, at the cost of more clusters without a village. Decrease
`minimumIslandRadius` to allow small islands to qualify as hosts. Increase
`minimumGap` to leave a wider void around villages.

In the versioned Create Sky preset, each anchor receives a deterministic allowance
of 0–3 villages. The weights are 96:24:8:3: approximately 73.3% receive zero, 18.3%
one, 6.1% two and 2.3% three. Companions share their anchor's allowance. These are
maximums, not promised counts: biome, layout and clearance rules can reject a site.
The profile owns this distribution and its candidate spacing; `enabled` controls
the horizontal neighbor filter, not the preset's count policy.

In ordinary Sky Archipelago presets, frequency remains owned by Sky Villages'
structure-set datapack. The neighbor rule only filters those candidates.

## BlueMap viewer

On a dedicated server with BlueMap 5.7, edit
`config/create_sky_compat-bluemap.json`. The mod default selects no maps. Add map
IDs to `maps` to opt in; only maps of the running Sky Archipelago Overworld qualify.
Both the ordinary Sky Archipelago generator and Create Sky Islands are supported.
The current world's actual vertical bounds are used. Nether, End and foreign
generators retain the original viewer.

| Field | Default | Accepted values |
| --- | --- | --- |
| `maps` | `[]` | Up to 128 nonblank BlueMap map IDs |
| `detailDistance` | 4000 | 1000–8000; camera-distance cutoff for detailed drawing |
| `tileRadius` | 31 | 1–48; maximum detailed tile radius |
| `concurrentRequests` | 4 | 1–8 concurrent detailed tile loads |
| `geometryMiB` | 256 | 32–1024 MiB of retained detailed vertex/index buffers |

All fields are required. Malformed settings disable the adaptation instead of
silently changing values. Run `bluemap reload` after edits; open browsers refresh
the adapter settings every 30 seconds. Empty `maps` restores the stock renderer.
These settings do not change saved generation identity or require a new world.
They do not change BlueMap's map boundaries, tile generation, resource consent or
network binding. Keep full-height rendering and cave culling disabled in BlueMap
when underside geometry is required. See [implementation and limits](development/bluemap.md).


## Distant Horizons

DH 3.3.3 is optional. The lifecycle fixes need no additional configuration.
Ordinary DH generation, server delivery and rendering retain their existing controls.

`<world>/serverconfig/create_sky_compat-dh-server.toml` contains
`savedWorldIdentity = false`. Enabling it uses a UUID saved in
`create-sky-lod-world-id` to distinguish Sky Archipelago saves even when their
seeds match. DH's `sendLevelKeys` must also be enabled. Foreign generators keep
DH's normal cache naming.

Leave this setting off to retain existing DH cache keys. Enabling it selects a
new client cache namespace; it does not migrate, delete or populate an old cache.
Once a save has an identity marker, that identity remains in use even if the
setting is reset. Keep the marker with the save when copying or restoring it.
Malformed markers fail explicitly instead of selecting an unrelated cache.

Clients with Compat do not import ambiguous legacy dimension-only caches into
these UUID namespaces. Existing legacy folders are left untouched; other DH keys
retain upstream migration behavior. Server databases and ordinary dimension IDs
are not renamed.

See [DH integration](development/lod.md) for exact hook boundaries and update checks.

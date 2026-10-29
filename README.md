# Create Sky Compat

Optional terrain, gameplay, and rendering adaptations for Minecraft 1.21.1.
Every adaptation has a startup switch. The Create Sky modpack is not required.

## Installation

Install this mod with **Java 21 and NeoForge 21.1.264 or later in the 21.1 series**.
No content mod is required. With no supported targets installed, no adapters activate.
Use it on the client for rendering fixes; install it on both sides with matching
configuration when using the expanded island preset.

Edit `config/create_sky_compat-features.properties` before startup to disable
unwanted features. Changes require a full process restart. Disabled adaptations
select no target mixins, callbacks, or data packs and do not constrain target
versions. An enabled adaptation activates only when its full dependency combination
is present on the applicable side; unsupported active combinations fail clearly.
See [all switches and supported combinations](docs/configuration.md#adaptation-switches).

| Optional integration | Supported version | Behavior |
| --- | --- | --- |
| Sky Archipelago | 1.3.6 | Expanded island preset and separately selectable placement/climate adaptations |
| Sky Villages + Architectury | 1.0.6 + 13.x (tested 13.0.11) | Village clearance and placement; needs Sky Archipelago |
| Sable | 2.0.6 | Island pressure; client single-block model-data correction |
| Aeronautics Delivery Quests | 1.1.1 | Separate route distance, chunk loading, and village landing adaptations |
| Analog Audio + Create | 0.1.5-hotfix.1 + 6.0.10 | Radio movement-handler registration |
| Distant Horizons | 3.3.3 | Client handoff, island shutdown, and opt-in cache isolation |
| BlueMap | 5.7 | Opt-in island viewer adaptation on dedicated servers |
| Veil + Iris | 4.3.2 + 1.8.14-beta.1+mc1.21.1 | Framebuffer restoration, including offscreen scenes |
| Aeronautics + Iris | 1.3.2 + the Iris version above | Exclude the balloon hot-air overlay from shadow rendering |
| Sable + Iris + Sodium | 2.0.6 + the Iris version above + 0.8.13+mc1.21.1 | Shader block IDs and single-block render passes |

Remove Veil Iris Fix when enabling the incorporated framebuffer adaptation.
Remove Sky Village Neighbors when enabling Compat's village adaptations.
Upstream mods retain their own dependencies. Create/Propulsion ore IDs are optional
members of the relative-placement tags, not required mods.

No world preset is selected automatically. To use the expanded terrain, install
Sky Archipelago and create a new world with **Create Sky Islands**, or set
`level-type=create_sky_compat:islands_v1` before creating a dedicated-server world.
Do not change an existing save's generator or recorded generation configuration.
Ordinary Sky Archipelago worlds receive only applicable enabled adaptations.

The rendering adapters have background checks and native macOS shader, Ponder,
balloon, and single-block glass comparisons. The original flickering/gapped airship
shadow report remains unresolved; this is not a general shadow correction.
The Analog Audio registration fix likewise does not establish moving playback or
GUI behavior. See [verification](docs/development/verification.md) for exact scope.

## Distant terrain

DH generates and renders distant terrain through its ordinary pipeline. Compat
adds focused lifecycle fixes and optional per-save cache isolation; it does not
provide a separate terrain predictor. Existing DH cache keys
remain unchanged by default. See [DH configuration](docs/configuration.md#distant-horizons)
and [integration boundaries](docs/development/lod.md).

## Island terrain

**Create Sky Islands** uses a build range from Y=-128 through Y=767. Clusters have
large anchor islands surrounded by smaller companions. Default anchor height
targets are 250 to 400 blocks, with placement fitted to leave floor clearance and
building headroom. Ore placement, large iron/copper veins and classic cave carvers
use island-relative heights rather than vanilla world altitude. The preset retains
stacked exterior terrain and offers baseline, noise-cave and hollow interiors with
separate controls for openings and native carving. Local basins can retain water,
overflow into channels or feed falls through deliberate underside openings.
Ancient-city eligibility uses a consistent
Deep Dark column query, and structure lookup resolves relocated starts instead of
rejecting them from their original source biome. Vanilla terrain systems are not
all reproduced; the [coverage limits](docs/development/terrain.md#feature-coverage-and-limits)
describe ocean content, fluids and native feature behavior.

The default candidate grid is 2400 blocks, with up to 750 blocks of displacement
in each horizontal axis. Accepted centers do not follow fixed spacing;
activation, biome eligibility, clearance and island shapes affect
actual travel distances. See [configuration](docs/configuration.md) for controls
and [terrain](docs/development/terrain.md) for implementation limits.

## Placement

The default rule requires at least **64 blocks** between the village's full
horizontal bounds and predicted island terrain. It also requires a sampled terrain
column within **512 blocks** belonging to an island with a native radius of at
least **96 blocks**. These distances are measured between block columns, not from
the village center.

The expanded preset shifts villages into its island altitude band and assigns
each archipelago zero to three villages, weighted toward zero. Buildings and loot
remain unchanged. Biome and terrain rejection can reduce the actual count. The
neighbor rule does not move villages onto islands or create roads. Ordinary Sky
Archipelago presets keep their original height and frequency. Existing generated
villages and regular villages are unaffected by the neighbor rule.

The host search uses a 16-block grid. It may miss a narrow eligible part of an
island and reject that candidate. Clearance checks every column in the exclusion
band, so that approximation cannot admit a village overlapping terrain. Terrain
predictions do not include later structures or player-built blocks.

## Configuration

Feature switches live in `config/create_sky_compat-features.properties`.
Edit active village tuning in `config/create_sky_compat-common.toml`, or use
**Mods → Create Sky Compat → Config** in a client. Island geometry lives in
`config/create_sky_compat-generation.json`; cavity and native-carver controls live in
`config/create_sky_compat-interiors.json`, and local basins use
`config/create_sky_compat-water.json`. Restart the game/server after changes;
changed generation settings require a fresh world.
The server's configuration controls multiplayer generation.

See [configuration](docs/configuration.md) for controls and limitations,
[development](docs/development/README.md) for builds and verification, and
[publishing](docs/publishing.md) for release instructions.

## License

Original implementation: MIT. Incorporated framebuffer code and its test infrastructure:
LGPL-3.0-only. See [NOTICE](NOTICE) and [license texts](licenses/). Upstream binaries
and templates are not bundled. This is an independent compatibility mod.

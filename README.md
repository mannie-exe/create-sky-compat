# Create Sky Compat

Adds a Sky Archipelago world preset with deep islands, island-relative geology
and optional village, physics and delivery integrations. It can be installed on
its own with the required dependencies; the Create Sky modpack is not required.

## Installation

For **Minecraft 1.21.1, Java 21 and NeoForge 21.1.264 or later in the 21.1 series**,
install Sky Archipelago **1.3.6** and this mod on both client and server.

Optional integrations activate when their dependencies are installed:

| Integration | Supported version | Behavior |
| --- | --- | --- |
| Sky Villages and Architectury API | 1.0.6 and Architectury 13.x (tested 13.0.11) | Floating-village clearance, altitude and cluster allowance; both mods must be present |
| Sable | 2.0.6 | Island-altitude pressure profile for its physics system |
| Aeronautics Delivery Quests | 1.1.1 | Structure-route loading and distance fixes; village surface retry also requires Sky Villages and Architectury |
| Analog Audio and Create | 0.1.5-hotfix.1 and 6.0.10 | Restore registration of the upstream radio movement handler; both exact versions must be present |
| Create | Installed feature IDs | Island-relative zinc and Overworld striated deposits |
| Create Propulsion | Installed feature IDs | Island-relative platinum |
| BlueMap | 5.7, dedicated server only | Opt-in browser detail and void-edge adaptation for Sky Archipelago maps |
| Distant Horizons | 3.3.3 | Client-wrapper lifecycle and shutdown fixes; optional per-save client cache isolation |

Aeronautics uses the Sable pressure integration; this mod does not require
Aeronautics or patch its controls. Optional integrations retain their own upstream
dependencies. Missing Create/Propulsion features are skipped. Iris and Veil are not
dependencies; this mod does not patch shader rendering or Ponder.
DH is optional. Its internal hooks target 3.3.3; other versions are rejected when
installed. Review these hooks before changing the pin.

The Analog Audio adapter repairs registration only. Its ordinary Create contraption
GUI and audible playback during movement still need client playtesting; Sable
airship audio uses a separate upstream integration. See the
[adapter maintenance procedure](docs/development/README.md#analog-audio).

Use the same version on client and server for the expanded preset.
Create a new world with **Create Sky Islands**, or set
`level-type=create_sky_compat:islands_v1` before creating a dedicated-server world.
Do not change an existing world's generator. Ordinary Sky Archipelago worlds
retain the neighbor filter without receiving the expanded terrain.

Remove Sky Village Neighbors when installing this replacement. The old common
configuration is copied only when the new configuration is absent. Leave Sky
Archipelago's custom structure rules enabled. Unsupported versions of pinned
gameplay integrations are rejected when installed. Unsupported BlueMap versions
retain their original viewer.
The minimum installation needs only Minecraft, NeoForge, Sky Archipelago and
Create Sky Compat.

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

Edit village settings in `config/create_sky_compat-common.toml`, or use
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

MIT. This is an independent compatibility mod, not an official release from either
upstream project. Neither upstream mod nor its templates are bundled.

# Create Sky Compat

Integrates Sky Archipelago terrain and floating villages for Create Sky.
The island preset adds deeper terrain and relative geology; see the
[terrain contract](docs/development/terrain.md) for scope and verification limits.
The existing village rule keeps structures beside islands with a configurable
horizontal gap.

## Installation

For **Minecraft 1.21.1 with NeoForge 21.1.264 or later in the 21.1 series**,
install Sky Archipelago **1.3.6** and this mod on both client and server.

Optional integrations activate when their dependencies are installed:

| Integration | Supported version | Behavior |
| --- | --- | --- |
| Sky Villages and Architectury API | 1.0.6 and Architectury 13.x (tested 13.0.11) | Floating-village clearance, altitude and cluster allowance; both mods must be present |
| Sable | 2.0.6 | Island-altitude pressure profile for its physics system |
| Aeronautics Delivery Quests | 1.1.1 | Structure-route loading and distance fixes; village surface retry also requires Sky Villages and Architectury |
| Create | Installed feature IDs | Island-relative zinc and Overworld striated deposits |
| Create Propulsion | Installed feature IDs | Island-relative platinum |

Aeronautics uses the Sable pressure integration; this mod does not require
Aeronautics or patch its controls. Optional integrations retain their own upstream
dependencies. Missing Create/Propulsion features are skipped. Iris and Veil are not
dependencies of this mod; the separate Veil Iris Fix owns the Ponder correction.

Use the same version on client and server for the expanded preset.
Create a new world with **Create Sky Islands**, or set
`level-type=create_sky_compat:islands_v1` before creating a dedicated-server world.
Do not change an existing world's generator. Ordinary Sky Archipelago worlds
retain the neighbor filter without receiving the expanded terrain.

Remove Sky Village Neighbors when installing this replacement. The old common
configuration is copied only when the new configuration is absent. Leave Sky
Archipelago's custom structure rules enabled. Unsupported versions of the pinned integrations are rejected when installed.
The minimum installation needs no village, physics, Create or delivery mods.

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
`config/create_sky_compat-generation.json`. Restart the game/server after changes;
changed generation settings require a fresh world.
The server's configuration controls multiplayer generation.

See [configuration](docs/configuration.md) for controls and limitations,
[development](docs/development/README.md) for builds and verification, and
[publishing](docs/publishing.md) for release instructions.

## License

MIT. This is an independent compatibility mod, not an official release from either
upstream project. Neither upstream mod nor its templates are bundled.

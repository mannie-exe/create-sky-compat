# Sky Village Neighbors

Keeps Sky Villages floating beside Sky Archipelago islands, with a configurable
horizontal gap. Villages above island terrain or far out in the void are rejected.

## Installation

For **Minecraft 1.21.1 with NeoForge**, install this mod with:

- Sky Archipelago **1.3.6**
- Sky Villages **1.0.6** and its Architectury API dependency

Install on the server, or in your client instance for singleplayer. Multiplayer
clients do not need this compatibility mod. Use the Sky Archipelago world preset
and leave its custom structure rules enabled. Other versions of the two mods are
intentionally rejected until their placement implementation has been checked.

## Placement

The default rule requires at least **64 blocks** between the village's full
horizontal bounds and predicted island terrain. It also requires a sampled terrain
column within **512 blocks** belonging to an island with a native radius of at
least **96 blocks**. These distances are measured between block columns, not from
the village center.

Villages keep their existing height, buildings and loot. The rule filters candidate
layouts; it does not move them onto islands, create roads or guarantee a village
count per cluster. Existing generated villages are unchanged. Other structures,
including regular villages, are unaffected.

The host search uses a 16-block grid. It may miss a narrow eligible part of an
island and reject that candidate. Clearance checks every column in the exclusion
band, so that approximation cannot admit a village overlapping terrain. Terrain
predictions do not include later structures or player-built blocks.

## Configuration

Edit `config/sky_village_neighbors-common.toml`, or use **Mods → Sky Village
Neighbors → Config** in a client. Restart the world or server after changes.
The server's configuration controls multiplayer generation.

See [configuration](docs/configuration.md) for controls and limitations,
[development](docs/development/README.md) for builds and verification, and
[publishing](docs/publishing.md) for release instructions.

## License

MIT. This is an independent compatibility mod, not an official release from either
upstream project. Neither upstream mod nor its templates are bundled.

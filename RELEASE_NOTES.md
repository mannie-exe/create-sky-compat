# Create Sky Compat 0.14.0

Add optional adaptations for Redstone Link GUI 1.21.1-1.9.2 and Redstone Additions
1.0.5, each with independent startup switches and configuration catalogue entries.
Neither mod becomes required; upstream JARs remain unchanged.

Link editing now checks player permissions, current menu/channel, original target
identity, valid indices and range on the server. Sable access uses projected world
positions. Relocation is limited to ordinary Redstone Links without factory-panel
connections, within one world or construct frame; unsupported moves are rejected.

Additions repairs cover Conjunctor placement and settled ticks, Crossroad feedback
and coherent state updates, support loss, unpowered Disjunctor defaults, Create
wrench interactions, successful-only verdigris scraping, and the lit inverter's
cutout render layer. Crossroad strong output through solid blocks loses one level;
wire behavior retains its upstream contract. Missing loot and language resources
remain the installing pack's responsibility.

Verification covers disabled-adapter controls, independent target absence,
minimum/full dedicated servers, native packet and block interactions, Sable
coordinate frames, client menus and Ponder scenes. See the redstone development
reference for limits, resource ownership and dependency-update considerations.
Terrain, saved generation identity and LOD cache policy are unchanged.

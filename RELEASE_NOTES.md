# Create Sky Compat 0.2.0

Replaces Sky Village Neighbors for Minecraft 1.21.1 / NeoForge 21.1.264+ (21.1 series).
Remove the old mod; its configuration is imported when the new file is absent.

- Add the Create Sky Islands preset with build bounds -128 to 767, deep anchor
  islands, smaller companions and island-relative Overworld geology.
- Adapt classic caves, selected underground features, cave biomes and climate to
  the island band; preserve unrelated generators and Nether/End generation.
- Keep floating villages beside islands, shift their altitude and assign each
  archipelago a weighted allowance of zero to three villages.
- Apply a scoped Sable pressure curve and bounded ADQ endpoint-loading, distance
  and village-surface corrections when their dependencies are installed.
- Require only Sky Archipelago 1.3.6 in addition to Minecraft/NeoForge. Village
  integration requires both Sky Villages 1.0.6 and Architectury 13.x; Sable 2.0.6
  and ADQ 1.1.1 are optional. Create/Propulsion feature references are optional.

Install on client and server. The new preset requires a new world; existing
ordinary Sky Archipelago worlds retain the neighbor filter. Saved generation
identity rejects incompatible changes rather than converting worlds.

Native checks cover minimal/full dedicated installations and reopening, terrain
queries, sampled ores and villages, climate, pressure and a controlled powered
balloon crossing. Survival operating costs, completed deliveries and wing-borne
flight remain unverified. Vanilla noise caves, aquifers and large noise ore veins
are not restored. See docs/development/verification.md for exact scope.

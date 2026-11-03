# Create Sky Compat 0.15.0

Add two independent client adaptations for glass face culling:

- `create.window_culling` keeps faces visible between different Create window
  materials and restores their native connected-texture context. Matching windows
  retain hidden interfaces. Requires Create 6.0.10.
- `rechiseled.glass_culling` preserves faces between differently oriented instances
  of the same Rechiseled glass pillar, including Rechiseled Create windows. Matching
  orientations and native slab/stair rules remain unchanged. Requires Rechiseled 1.2.6.

Both default on when their supported target is installed. They have separate
Client configuration groups and select no hooks on dedicated servers. Neither
requires Fusion APIs, Iris or island generation; the content mods' own dependencies
still apply. Native models, textures and Fusion connection predicates remain intact.

The isolated resource probe now records world-aware CT quads and face visibility
and can run without Iris. It remains excluded from the distributed mod.

See the maintained verification guide for artifact hashes, native cases and limits.

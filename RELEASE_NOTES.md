# Create Sky Compat 0.6.1

Fix single-block construct rendering with Iris shaders. Sable queued translucent
single-block geometry outside its matching terrain pass, making ordinary glass
and glass Copycat constructs disappear while stationary blocks remained visible.
The new `rendering.single_block_passes` startup switch independently controls the
correction. It activates only with the supported Sable, Iris, and Sodium versions.

Native comparisons reproduced missing glass in 0.6.0 and restored both construct
types in 0.6.1. Ponder, shader toggling/reload, valid heated balloons, and material
controls were reviewed on macOS arm64 with Complementary Unbound r5.9.3 and
Euphoria Patches 1.10.5. See [verification](docs/development/verification.md) for
artifacts, commands, controls, and limits. The original gapped/flickering airship
shadow report remains unresolved.

All integrations remain optional and independently configurable. Terrain,
recipes, weights, and world identity are unchanged. Remove standalone Veil Iris
Fix when using the incorporated framebuffer adapter.

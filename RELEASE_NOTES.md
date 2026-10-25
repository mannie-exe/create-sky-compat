# Create Sky Compat 0.4.0

Changes the existing Create Sky Islands preset (`create_sky_compat:islands_v1`).
A fresh world is required; saved generation identity rejects incompatible terrain.

- Bring clusters closer with 2400-block cells, 90-block jitter and unchanged 0.85
  activation. Complete terrain supports retain at least 354 blocks of clearance.
- Select a uniform main-island height target from 250 to 400 blocks. Fit each
  island's elevation to its chosen body depth while preserving building headroom.
- Replace the native underside density jump with a continuous curved boundary.
  Preserve native archetypes, erosion and nominal horizontal dimensions.
- Keep geology tied to each selected body. Companions share their host's frame.
- Validate generated support and headroom when the native fresh-world fallback
  chooses a spawn; carving and features can differ from its predicted surface.

Geometry JSON now uses `anchorMinHeight`, `anchorMaxHeight` and `undersideTaper`
in place of `anchorCliff`. Install matching configuration on client and server.
The smaller edge-gap requirement replaces the previous 1500-block minimum.
Measured heights and shore distances are samples, not survival mining or travel
budgets. See development verification for tested artifacts and limits.

Minecraft 1.21.1, NeoForge 21.1.264+ within 21.1, and Sky Archipelago 1.3.6 remain
required. Other integrations remain optional. Ordinary Sky Archipelago, Nether,
End, Sable pressure and rendering are unchanged by this pass.

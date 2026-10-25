# Create Sky Compat 0.3.0

Changes the existing Create Sky Islands preset (`create_sky_compat:islands_v1`).
A fresh world is required; saved generation identity rejects incompatible terrain.

- Vary anchor centers across Y=242–490, with independently fitted companions,
  floor clearance and at least 200 blocks of terrain headroom.
- Separate full cluster envelopes by at least 1500 blocks using a 4100-block grid
  and bounded jitter. Sampled shore crossings are usually longer, around 3 km.
- Map vanilla underground depth to each anchor's body, with reference Y=64 at its
  surface. Keep shallow canyon origins relative to the actual local surface.
- Adapt fossils, underground clay and jungle vines; preserve native feature checks.
- Follow host altitude for floating villages. Keep biome climate independent of
  altitude and consistent between client weather and server freezing.
- Expose one validated JSON geometry profile; preserve optional integration gates,
  foreign generators, the continuous Sable pressure curve and saved-world guards.

Minecraft 1.21.1, NeoForge 21.1.264+ within 21.1, and Sky Archipelago 1.3.6 remain
required. Other supported integrations remain optional. Install matching versions
and geometry configuration on client and server. This does not restore vanilla
noise caves, aquifers or large noise veins. See development verification for
sampled results and remaining gameplay limits.

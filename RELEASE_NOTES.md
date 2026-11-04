# 0.19.0

- Keep text displays and entity names readable with independent, default-on Iris
  lighting adaptations. Selected text uses Iris fallback programs while ordinary
  geometry keeps its shader lighting. Later shader color processing still applies.
- Enable the separate text-display and nametag cast-shadow exclusions by default.
- Center the measured footer hint against the configuration screen's button row.

Minecraft 1.21.1, NeoForge 21.1.264. Rendering adapters target Iris
1.8.14-beta.1+mc1.21.1. See [verification](docs/development/verification.md) for native
shader checks and coverage limits. No generation or world-format changes.

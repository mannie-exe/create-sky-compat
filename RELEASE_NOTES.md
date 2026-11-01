# Create Sky Compat 0.11.1

Add optional Transmission & Linkage 0.2.9 adaptations with Create 6.0.10,
Sable 2.0.6 and Simulated 1.3.2:

- Sneak-use a Create wrench on a visible rod to detach it. Ordinary attacks and
  bare-hand sneak-use no longer disconnect rods accidentally.
- Validate player edits, settings packets, wrench removal and both binding
  endpoints on the server. Preserve targeting checks and native link/save data.
- Add optional Jade link, actuator and conversion-output inspection. Jade remains
  optional; inspection reads server state and existing suppression results.

Native actuation, power limits, recipes and terrain generation remain unchanged.
Stock rod lengths avoid upstream's enlarged-range visual limit. See
[Transmission & Linkage](docs/development/transmission-linkage.md) and
[verification](docs/development/verification.md) for the tested scope and limits.

The Ponder resource checker reads UTF-8 explicitly on every platform, including
Windows. This release includes an encoding-independent generator check.

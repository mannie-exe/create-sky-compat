# Create Sky Compat 0.10.1

Add optional Linear Bearing guidance alongside the existing machinery repairs:

- Six Ponder tutorials cover assembly, external motion and braking, recovery,
  magnetic ports, spring anchors, and angular-rate sensing. They use staged
  reveals, interaction prompts, animations and localized English text.
- Jade shows server-observed brake and transmission-link state, with detail-key
  hints for assembly, recovery and the difference between sensing and driving.
- Independent startup switches control both integrations. Ponder requires the
  active carriage adapter and the reviewed Ponder/Create pair; Jade requires
  that adapter and the reviewed Jade version. Transmission and gyro guidance
  follows its owning adapter. Minimum installation remains NeoForge plus Compat.

The original passive-guide, brake, transmission and sensor contracts are
unchanged. Terrain, saved generation identity, rendering, lighting, Questlog,
DH and BlueMap adaptations retain their existing behavior.

See [machinery guidance](docs/development/linear-bearing.md#ponder-and-jade) and
[verification](docs/development/verification.md) for tested scope and limitations.
Modrinth publication remains deferred.

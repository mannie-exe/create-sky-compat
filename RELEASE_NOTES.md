# Create Sky Compat 0.9.0

Add four optional adaptations for Linear Bearing 1.3.5:

- Preserve safe dedicated-server registrations while removing client-only startup
  linkage, and supply the missing bubble-gum drop. With Linear Bearings Fix 1.0.3
  installed, it remains the sole owner of these overlapping repairs.
- Check assembly before converting the bearing, engage a relative carriage brake
  through current Sable/Simulated APIs, and recover aligned, braked machinery
  without overwriting occupied destinations or losing cargo.
- Connect magnetic ports and spring anchors through Create's actual kinetic
  network. Loads use the real drive's stress budget; endpoints generate no power.
- Make the gyroscopic gearbox a zero-capacity angular-rate sensor and let its
  reading settle to zero after stopping.

The passive guide, recipes, yields, weights and other utility mechanics retain
their existing roles. These changes do not add a powered linear actuator or an
automatic stabilizer. Independent startup switches control each adaptation;
Create 6.0.10, Sable 2.0.6 and Simulated 1.3.2 are the reviewed machinery targets.

Existing Colorwheel and SDL/Lamb lighting repairs remain included. Terrain,
Questlog, DH, BlueMap, rendering and saved generation identity are unchanged.
No SeedGen integration is added. Hoofprint zoom remains a documented investigation,
with no applied adapter or settings changes.

See [machinery contracts](docs/development/linear-bearing.md) and
[verification](docs/development/verification.md) for native loading, physics,
recovery, reload and client checks. Old upstream injected-source network saves,
arbitrary third-party assembly exceptions and every aircraft arrangement are
outside those fixtures. Modrinth publication remains deferred.

# Create Sky Compat 0.12.0

Add two independently configurable adaptations:

- Analog Audio 0.1.5-hotfix.1 + bundled AnalogLib 0.1.0: join the server media
  timeline after loading, reconcile standby playback and guard obsolete async
  work. Client-only; Create is not required. Native volume, attenuation,
  filtering, positioning and media decoding remain in use.
- Sable 2.0.6 + You're in Grave Danger 2.0.13: place graves in real construct
  plots, retain recovery through removal/unloading, use bounded loss fallback
  and project tracking into world coordinates. YiGD remains the only inventory
  and experience authority. Create, Aeronautics and island generation are optional.

Two-client playback and native minimum/full-pack recovery checks passed. A real
client exercised natural deck support, death/respawn, ray targeting, owner claim,
backpack contents/equipment and vanilla dropped XP. See [verification](docs/development/verification.md#audio-and-construct-recovery-0120)
for artifact identifiers and limits, and [adapter maintenance](docs/development/adapters.md)
for dependency-update requirements.

Terrain generation and existing LOD identities are unchanged. Keep the recovery
adapter enabled while using its saved construct graves. Initial internet loading
can remain silent; this is not sample-perfect cross-client playback or universal
claims-mod integration.

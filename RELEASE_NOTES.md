# Create Sky Compat 0.13.0

Configure adaptations through a category and mod sidebar. Client, Server / shared,
World generation, and Status & files replace the separate configuration entrances.
Individual fixes retain their existing keys, defaults, dependencies, and side rules.

Journal preferences apply immediately through either entrance. Startup fixes use
Save changes; current activation, saved next-start values, and unsaved edits remain
separate. Missing targets stay inactive. Details contain dependency versions,
prerequisites, file locations, and defaults without repeating them in every row.

The startup editor preserves unrelated properties and omitted defaults, refuses
stale overwrites, and replaces valid files atomically. Failed writes retain drafts.
Generation edits require confirmation and retain saved-world identity safeguards.
The village editor also rejects inverted minimum/maximum distances before saving.

Use wrapped rows, independent scrolling, native keyboard controls, and contextual
narration. The journal bookmark keeps its existing skin and parent-screen state.
No new UI dependency or configuration migration is required.

Minimum and full-pack clients and dedicated-server combinations were checked.
See [verification](docs/development/verification.md) for exact artifacts and limits,
and the [editor reference](docs/development/configuration.md) for update procedures.
Generation, gameplay adaptations, and LOD identities retain their existing behavior.

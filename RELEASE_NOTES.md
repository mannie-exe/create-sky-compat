# Create Sky Compat 0.5.1

Add an optional Distant Horizons 3.3.3 integration on Minecraft 1.21.1.
Terrain generation, saved generation identity and existing non-DH adapters are
unchanged from 0.5.0. Existing worlds do not need regeneration.

- Publish replacement client-level wrappers before retiring their predecessors;
  delayed cleanup cannot remove a newer wrapper.
- Bound Sky Archipelago shutdown unload passes so queued generation cleanup can
  run between passes. Ordinary running-server unloads remain unchanged.
- Offer opt-in per-save DH cache keys. The default preserves existing keys and
  caches. Enabling isolation selects a new cache without deleting or migrating
  old data; once created, the save identity remains authoritative.
- Keep DH optional and client hooks off dedicated servers. No SeedGen integration,
  prediction worker or replacement LOD generator is included.

The development guide documents a DH 3.3.3 glow-lichen tint/material transfer
that can make distant cliffs or fluid segments luminous. A per-block DH exclusion
is the verified workaround; this mod does not rewrite operator settings.

Verification includes 47 Java and five release-tool tests, package/upstream
checks, minimum-install native generation, and full-pack shutdown during active
DH generation with the ordinary cache key preserved. Physical-client checks
cover cache construction, lifecycle and fluid/material conversion; the verification
guide records exact versions, artifacts and harness limitations. Concurrent tests
exercise ownership interleavings; runtime checks do not deterministically reproduce
every race. No performance improvement or complete renderer compatibility is claimed.

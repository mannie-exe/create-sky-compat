# Distant terrain integration

Investigation dated 2026-10-26. No new LOD adapter is implemented or installed.
Create Sky Compat 0.5.0 retains ordinary DH generation and networking. The current
pack uses DH 3.3.3 with CHUNKS_ONLY / INTERNAL_SERVER: real chunks are generated,
then reduced to distant rendering data. Pregenerating chunks, obtaining LOD data,
transferring it and rendering it are separate operations.

## Existing renderers

Distant Horizons renders simplified terrain beyond the native chunk view. Its
installed API can accept direct LOD data through `IDhApiWorldGenerator.generateLod`
and `API_DATA_SOURCES`, without requiring an adapter to generate full chunks.
The method is present in the exact 3.3.3 JAR, not just the current
[API documentation](https://distant-horizons-team.gitlab.io/distant-horizons/com/seibel/distanthorizons/api/interfaces/override/worldGenerator/IDhApiWorldGenerator.html).
Air, lighting and vertical spans matter: a surface height alone cannot describe
stacked islands, undersides or hollow interiors. Pooled data belongs to DH and
must not escape its task lifetime.

[Voxy](https://modrinth.com/mod/voxy) is a different renderer and cache system.
The official distribution targets Fabric, has no 1.21.1 release, requires OpenGL
4.6 and needs explicit shader-pack support. It ingests explored terrain or existing
worlds; external generators are a separate concern. Its official inclusion policy
permits published official versions, not redistributed unofficial backports.

[Roxy](https://modrinth.com/mod/roxy) 0.4.1 provides a NeoForge 1.21.1 translation
layer around the original Voxy 0.2.16-beta Minecraft 1.21.11 artifact. Its author
lists Create, Sable and Iris support. Apple Silicon support requires an additional
Zink/KosmicKrisp graphics bundle and per-instance wrapper; it does not run through
stock Apple's OpenGL path. The [setup guide](https://github.com/RasaNovum/Roxy/blob/main/Roxy-macOS-compat/setup-guide.md)
requires macOS 26+, ARM64 Java 21 and a separate clean instance. The inspected host
runs macOS 26.7.1, but this renderer stack has not been tested here. The guide's
0.4.1 versus greater-than-0.4.1 wording needs checking against the selected build.
A whole-renderer experiment must separately cover shaders, Veil/Ponder, dynamic
Sable sublevels and resource reload; advertised compatibility is not pack evidence.

## DH SeedGen 0.9.1

[SeedGen](https://modrinth.com/mod/distant-horizons-seedgen) supplies predicted
terrain to DH rather than replacing its renderer. The author describes faster
seed-based sampling with approximated trees and structures, plus multiplayer
seed sharing. Custom generators and terrain-changing mixins are explicitly outside
the general noise-based compatibility claim. Reported speedups are the author's
benchmarks, not measurements on this pack. The author also reports device-dependent
conflicts with C2ME and Fast Noise; both are present in Create Sky.

The inspected [NeoForge 1.21.1 artifact](https://modrinth.com/mod/distant-horizons-seedgen/version/2DrfEi0d)
has mod ID `seedrenderer`, version 0.9.1 and SHA-512
`4b0bbb0426d0d600fcef2deb0580f458bbf16b90440b84b9d28fe377a3107cf22ff9f166eec85b802cb5386d5af39d5b0a12ac6bf7ca04fad427206d1405c61d`.
Its metadata accepts NeoForge 21.1 and declares DH >=3.2 optional, idling without
it. Modrinth lists no public source repository. Inspection used the released
bytecode; downloaded/decompiled files are disposable and are not redistributed.

Observed implementation boundaries:

- `WorldBasis` captures seed, registries, noise settings, biome source and random
  state. `SeedColumnSampler` samples density/height fields at the requested stride
  instead of invoking the full chunk-generation pipeline.
- `SeedWorldGenerator` registers through DH's world-generator override API and
  supplies API data sources. `ColumnWriter` encodes and merges vertical material,
  biome and light spans. Feature readers contribute visual trees/structures; their
  presence does not establish identical native feature placement or simulation.
- `WorldgenCompat.overriddenTerrain` compares a noise-generator subclass against a
  new standard noise generator at twelve near/far columns. Heights must agree
  within 24 blocks at all but one sample. The server binding stands down when this
  check fails. Sky Archipelago's final `SkyIslandChunkGenerator` extends the noise
  class but supplies custom terrain, making this a relevant refusal path. No
  runtime SeedGen/Sky Archipelago trial has been performed.
- Multiplayer `SeedSharing` compares terrain-mod lists and a worldgen registry
  fingerprint. Its payload carries dimension, seed and a noise-settings key.
  `WorldBasis.fromSeed` then creates a standard `NoiseBasedChunkGenerator`, not the
  original generator subclass. The external Compat geometry/interior/water JSON
  files and saved generation identity are not carried by that payload.
- `shareSeedWithClients` and `serverGeneration` default true. A future integration
  must explicitly decide whether a server distributes its seed; exact server LOD
  delivery does not require giving the seed to clients. The manual server-seed
  path is described as vanilla-only, so it is not a workaround for this pack.

Removing a compatibility rejection would not make our density composition,
clearance, island-relative geology, biome projection or local water appear in
SeedGen's reconstructed generator. Equal seeds alone are insufficient. A misleading
preview could persist until actual visited terrain replaces it. SeedGen's own
persistence, scheduling and renderer hooks also need conflict review; it is more
than one public terrain-provider interface. No stable third-party generator API
was identified in the inspected artifact.

## Proposed direction

Prefer an optional **DH terrain provider owned by Compat**, with a small optional
SeedGen coexistence adapter if needed, over replacing SeedGen's internal sampler.
This is a design proposal, not a promise of measured speed or complete fidelity.

1. Scope registration to our marked preset and the supported DH version. Reuse the
   generation profile and resolved cluster/column interfaces. Fall back to native
   DH outside that scope or when identity validation fails. SeedGen may retain
   ownership of other supported dimensions; do not register competing providers.
2. Produce bounded coarse island silhouettes first, preserving all relevant solid,
   water and air intervals from -128 through 767. Respect negative coordinates,
   cross-cell companions and actual biome/material ownership. Use conservative
   footprints to skip terrain sampling in void, not to delete saved constructions.
3. Refine silhouette data into island bodies, visible hollows and local water.
   Label native features, classic carvers, lighting and simulated fluid extents
   as remaining approximation boundaries until explicitly reproduced. Reusing our
   pure column resolver avoids a second, drifting implementation of terrain rules.
4. Give stored/generated real terrain precedence over predictions. Keep provenance
   and generation/config identity so changing the approximation cannot contaminate
   authoritative cache data. Verify DH replacement semantics rather than assuming
   that marking predicted data LIGHT makes later updates safe.
5. Address island-first request ordering separately. Faster generation alone does
   not change DH's nearest-first client download queues. Use bounded cached land
   hints, coarse coverage before refinement, and aging/fairness for every section.
   Bridges and other constructions in nominal void must still receive updates.

Start with server-side prediction through DH's existing transport. Client-side
prediction can reduce network traffic later, but needs an explicit handshake for
seed, generator identity, configs and registry content, plus mismatch refusal and
world-unload cleanup. Do not copy generation secrets or installation IDs into
pack defaults. The minimum installation must continue loading without DH or
SeedGen, and dedicated servers must not resolve client renderer classes.

Before adoption, compare empty/land/boundary/stacked/water columns against native
generation on multiple seeds, then test cold-cache join, real-data replacement,
reconnect, changed player blocks and absent/mismatched optional mods. Measure the
same workload with and without prediction, including memory, CPU and transfer;
fast empty-section generation alone is not an island-visibility benchmark. Use
separate disposable caches and worlds for these checks.

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

## Direction: Compat owns island LODs and the SeedGen fix

The accepted scope is an optional Create Sky Compat integration for ordinary and
modified Sky Archipelago terrain, plus correction of the SeedGen multiplayer
reconstruction bug. Do not revive Moxy or build another renderer. Its useful design
constraints are full generation identity, observed-data precedence, known-air
handling and bounded work; its existing renderer is not a qualified dependency.

There are two independent implementation boundaries:

| Boundary | Finding | Proposed responsibility |
| --- | --- | --- |
| Island sampling | SeedGen's sampler reads noise fields, while our resolved columns include custom clearance, taper, interiors and water | Compat supplies actual island intervals and material/biome context |
| Multiplayer admission | The server binding rejects unsupported subclasses, but `SeedSharing.offerLevel` only checks the noise-generator base type before sharing seed/settings; `onWorld` stores the reconstructed standard generator | Apply the same capability decision to sharing, carry validated custom context for supported worlds, and refuse incomplete reconstruction |

The second row is a concrete code-path asymmetry in the inspected 0.9.1 artifact,
not a completed runtime reproduction. The registry fingerprint does not replace
our saved generation marker or external JSON config digests. A narrowly scoped
first fix should prevent unsupported Sky Archipelago reconstruction on either
side. It must not globally suppress other generators or loosen the existing
compatibility check.

### Integration choices

An internal SeedGen bridge would adapt `WorldBasis.fromSeed`, its admission/binding
paths and the `SeedWorldGenerator`/`SeedColumnSampler` dispatch. The generator is
final and holds concrete thread-local samplers; there is no discovered public
provider interface to implement. Its Overworld path is not a general arbitrary
stacked-column renderer. Supplying one top height would lose island bottoms,
stacked bodies, exposed hollows and water intervals. This route retains more of
SeedGen's feature/scheduling machinery but couples Compat to several exact-version
internals. Feature anchors and the layer limit need explicit review too.

A narrower alternative is a Compat-owned `IDhApiWorldGenerator` registered with
DH for Sky Archipelago levels, plus a SeedGen adapter that routes/stands down only
on those levels and fixes its sharing guard. DH's installed API exposes direct
column data, explicit generation steps and override priority. This keeps SeedGen
usable on supported dimensions and avoids copying its implementation. It supplies
custom island LODs, not a competing renderer. The public API boundary is preferable
if internal SeedGen reuse cannot be kept small, but coexistence must be verified:
a registration priority alone does not control SeedGen's client retrieval hooks.

Both choices require the same island sampler and context contract. Decide the
binding after a bounded prototype compares emitted columns and lifecycle behavior;
do not commit to a large SeedGen rewrite merely to call the result an adaptation.

### Shared implementation requirements

1. Gate optional classes by mod presence, physical side and supported version.
   Retain upstream settings for ordinary Sky Archipelago and the complete saved
   geometry/interior/water/ocean identity for our preset. Other generators keep
   ordinary DH/SeedGen behavior.
2. Reuse resolved contributor/column intervals, including `ColumnData.segments`,
   `LayeredColumn` and water spans. Preserve all relevant solid, fluid and air
   intervals from -128 through 767. Start with coarse silhouettes and refine
   material/biome detail; conservative footprint tests can avoid sampling void.
3. Treat native structures, vegetation, classic carvers, fluid simulation and
   lighting as explicit fidelity boundaries. Pure column sampling does not run
   those later stages. Do not silently fill the underside down to the world floor
   or copy an arbitrary fixed maximum number of solid layers.
4. Keep stored/generated real data authoritative, including player edits and
   known air. Verify replacement and provenance semantics; marking a prediction
   LIGHT must not prevent later real data from replacing it. Config/reload/world
   identity changes invalidate predictions independently of real caches.
5. Use a versioned, bounded context exchange for client prediction: generator type,
   effective seed, configs, registry identity and generation marker. Respect the
   operator's seed-sharing policy. Reject unknown/mismatched context rather than
   falling back silently to vanilla noise. Clean up on disconnect/world unload.
6. Keep island-first request ordering a separate concern. Faster sampling alone
   does not change DH's nearest-first download queues. Prioritize coarse land
   coverage with fair service for every section; nominal void can contain builds.

Before implementation, reproduce the server-binding versus client-sharing case
with an isolated matched mod set. For the selected bridge, compare native and LOD
columns at void, boundaries, stacked islands, hollows and water across several
seeds. Then test cold-cache join, actual-data replacement, reconnect, edited blocks,
plain peers and missing/mismatched optional mods. Measure identical workloads,
including memory, CPU and transfer. Do not install SeedGen into the playing world
or reuse its live caches for these experiments.

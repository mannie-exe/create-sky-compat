# Adapter maintenance

[Configuration](../configuration.md#adaptation-switches) owns feature keys,
defaults, supported versions, and prerequisites. `FeatureSelection` is the code
owner of those boundaries. Loader metadata deliberately accepts optional versions;
only active adapters enforce supported versions. Build-time SHA-256 checks bind
implementation tests to the exact released artifacts.

| Adapter family | Implementation and interactions | Update and retirement checks |
| --- | --- | --- |
| Island preset | `TerrainIntegration`, terrain mixins, terrain built-in pack; dependent generation switches require `terrain.preset` | Review Sky Archipelago generator/field contracts, codecs, dimensions, and saved identity. Run native generation/reopen checks. Never substitute a saved generator. |
| Relative placements | `OreHeightMixin`, ores built-in pack | Review vanilla placements and optional Create/Propulsion IDs; disabling removes both the hook and tags. |
| Village clearance/placement | Separate hook selections; neighbor tuning only when relevant | Review Sky Villages + Architectury and Sky Archipelago validator order. Check ordinary/expanded terrain, footprint bounds, allowance, and disabled combinations. |
| Sable pressure | `DimensionPhysicsMixin`, pressure built-in pack | Review Sable physics profile codec and island-only selection; preserve upstream machine/block weights. |
| ADQ | Separate distance, chunk-loading, and landing mixins | Review private route lambdas, injection counts, ticket lifetime and cleanup. Landing requires Sky Villages but does not enable its placement adaptations. |
| Analog Audio | `AnalogAudioCreateMixin` | Review public Create registry erasure and upstream reflective lookup. Retire when released Analog Audio registers the handler correctly. See [procedure](README.md#analog-audio). |
| Radio timeline | Client clocks, native queue correction and guarded async callbacks | Review the exact bundled media engine, queue positions and session ownership; see [audio](audio.md). |
| Construct recovery | Server support sampling, YiGD placement metadata and lifecycle/tracking hooks | Review both mods' save, plot, death, claim, disassembly and compass paths; see [recovery](recovery.md). |
| Linear Bearing | Separate constructor/loot, carriage lifecycle, kinetic links and gyro hooks | Review [contracts and native fixtures](linear-bearing.md), exact constructor shape, repair ownership, Sable frames/handles and native Create accounting. |
| Transmission & Linkage | Optional server-authorized mutations, deliberate rod removal and Jade state | Review [packet, binding, protection and client input boundaries](transmission-linkage.md) against the exact release; preserve native physics/save ownership. |
| DH | Separate client handoff, shutdown, cache selections | Review ownership and dimension guards, ordinary/default keys, reconnect and shutdown. See [DH](lod.md). |
| BlueMap | Dedicated-server registration only | Review extension schema, map opt-in and renderer boundaries. See [BlueMap](bluemap.md). |
| Moving lights | `SodiumLightingMixin` and independent Create-owner cleanup, physical client; no terrain prerequisite | See [lighting](lighting.md). Retire each hook when SDL supports the selected Sodium cache API or clears its Create holders on world unload. |
| Colorwheel sampler allocation | Independent client repair when upstream disables OIT | Review slot ownership, OIT guards and Iris external-sampler contract; supported OIT remains unchanged. |
| Colorwheel materials | Dependent client writer for construct material scopes | Review direct Sable tessellation and Colorwheel terrain/emission encoding together; see [rendering](rendering.md#colorwheel). |
| Glass backfaces | Separate post-CT Create and post-processor Fusion client hooks | Review quad format, winding/normals, native cull groups, Sodium batching and Fusion modifiers; both switches default off. See [rendering](rendering.md#glass-backfaces). |
| Create windows | Client-only material-boundary correction in both native glass/window overrides | Review face suppression and CT-data collection together; preserve matching interfaces and shape occlusion. See [window faces](rendering.md#window-faces). |
| Rechiseled glass | Independent client-only axis-boundary correction in `RechiseledGlassPillarBlock` | Review both vertical and horizontal paths, the vanilla AXIS property, and native slab/stair shape handling. See [window faces](rendering.md#window-faces). |
| Framebuffer, balloon, material IDs, model data, render passes | Five independent client selections | See [rendering](rendering.md). These do not depend on island generation or each other. |

For a dependency update, inspect the exact release and any nested JARs before
changing pins. Compare the affected methods with the patch and its upstream issue.
Update compile/test artifact hashes, feature version gates, operator documentation,
and pack pins together. A merged PR does not establish that a released artifact
contains the complete fix. Remove only the superseded adapter and its switch,
resources, tests, and documentation; leave unrelated adapters intact. Document a
removed key before upgrading a pack because unknown configuration keys fail.

Run `./gradlew check build` and relevant disposable startup combinations. Include
an installed-but-disabled target and an unsupported-but-disabled target whenever
activation changes. Background startup and transformed-bytecode checks do not
replace native client checks when rendering behavior changes.

The pack owns static resource/model patches and its explicit feature selections.
Compat owns runtime hooks. The Atlas consumes runtime exports, not this adapter
reference; shader IDs are not recipe or weight data. Update the owning pack/app
pipeline only when its actual inputs or interpretation change.

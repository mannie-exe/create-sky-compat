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
| DH | Separate client handoff, shutdown, cache selections | Review ownership and dimension guards, ordinary/default keys, reconnect and shutdown. See [DH](lod.md). |
| BlueMap | Dedicated-server registration only | Review extension schema, map opt-in and renderer boundaries. See [BlueMap](bluemap.md). |
| Framebuffer, balloon, material IDs, model data | Four independent client selections | See [rendering](rendering.md). These do not depend on island generation or each other. |

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

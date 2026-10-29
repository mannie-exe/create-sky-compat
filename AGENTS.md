# Create Sky Compat

Maintain an independently installable mod of optional terrain and compatibility adaptations.
Keep minimum dependencies and optional adapters explicit.
Keep Sky Villages floating beside Sky Archipelago terrain. Preserve foreign
generators and saved-world generation identity; never silently convert worlds.
Own the incorporated Veil/Iris rendering adapters here; keep static pack resources in the pack.
Only Minecraft, NeoForge and necessary base libraries may be required.
Feature switches are immutable startup configuration. Disabled adapters must not
select target mixins, load foreign classes, expose data or constrain target versions.
Use Java 21, NeoForge and exact upstream dependency pins. Do not bundle upstream
binaries, templates or decompiled sources.

Optional integrations must remain optional at class-loading time and on each
physical side. Select hooks through loader metadata before resolving foreign
classes; Sky Villages requires both it and Architectury. Keep client rendering
out of dedicated-server paths. Verify the minimum installation and the complete
integration set; add intermediate combinations when a changed dependency boundary
needs them, rather than assuming one successful full-pack run covers absence.

Use saved generation identity and the island-relative geological frame as the
authority for placement and height adaptations. Keep tuning in its owning config,
not duplicated here. Test clipping, sparsity and resource availability across
multiple seeds; a spawn island or average distance does not establish all bounds.
Map/preview adapters must not generate chunks, activate gameplay or rewrite admin
settings implicitly. Isolate optional web-renderer behavior from terrain generation.

Keep geometry independent of game APIs and put loader integration in small
adapters. Worldgen must not load neighboring chunks or depend on visitation order.
Test boundary conditions, real Mixin application, native generation and cost.
`./gradlew check build` is required after implementation changes; passing unit
tests alone does not establish generation behavior.

README owns installation and behavior; docs own configuration, development and
publishing. Use direct prose without marketing. Preserve one owner per fact.
Remove disposable worlds and inspection files after recording useful evidence.
Do not edit live instances or publish releases without authorization.

Use conventional commit subjects with relevant `- [tag]: message` body bullets.
Keep bullet messages under 40 characters, lowercase except names, without a final
period. Scale detail to the change.

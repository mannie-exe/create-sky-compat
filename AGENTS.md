# Sky Village Neighbors

Keep Sky Villages floating beside Sky Archipelago terrain. Change only the
placement of `skyvillages:skyvillage`; preserve other structures and saved worlds.
Use Java 21, NeoForge and exact upstream dependency pins. Do not bundle upstream
binaries, templates or decompiled sources.

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

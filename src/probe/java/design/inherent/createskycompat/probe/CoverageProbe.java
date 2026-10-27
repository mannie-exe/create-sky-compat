package design.inherent.createskycompat.probe;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.nio.file.*;
import java.util.*;

/** Resolved data and native eligibility observations; no claim of structure realization. */
final class CoverageProbe {
    private CoverageProbe() {}

    static void run(MinecraftServer server, Path output) throws Exception {
        var report = new JsonObject();
        var level = server.overworld();
        var access = server.registryAccess();
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        report.addProperty("seed", level.getSeed());
        report.addProperty("generator", level.getChunkSource().getGenerator().getClass().getName());
        report.add("selectedPacks", new Gson().toJsonTree(server.getPackRepository().getSelectedIds()));
        var biomes = new JsonObject();
        var roots = new TreeSet<String>();
        for (var holder : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
            var settings = holder.value().getGenerationSettings();
            biomes.add(holder.getRegisteredName(), BiomeGenerationSettings.CODEC.codec().encodeStart(ops, settings).getOrThrow());
            settings.features().forEach(step -> step.forEach(feature -> roots.add(feature.getRegisteredName())));
        }
        report.add("overworldBiomes", biomes);
        report.add("topFeatures", new Gson().toJsonTree(roots));
        var placed = new JsonObject();
        var placedRegistry = access.registryOrThrow(Registries.PLACED_FEATURE);
        for (var id : new TreeSet<>(placedRegistry.keySet())) {
            var holder = placedRegistry.getHolder(id).orElseThrow();
            var row = new JsonObject();
            row.add("value", PlacedFeature.DIRECT_CODEC.encodeStart(ops, holder.value()).getOrThrow());
            row.add("tags", new Gson().toJsonTree(holder.tags().map(tag -> tag.location().toString()).sorted().toList()));
            row.add("configuredClosure", new Gson().toJsonTree(holder.value().getFeatures()
                    .map(feature -> feature.feature().getClass().getName()).distinct().sorted().toList()));
            placed.add(id.toString(), row);
        }
        report.add("placedFeatures", placed);
        var configured = new JsonObject();
        var configuredRegistry = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (var id : new TreeSet<>(configuredRegistry.keySet())) {
            var value = configuredRegistry.get(id);
            var row = new JsonObject();
            row.addProperty("class", value.feature().getClass().getName());
            row.add("value", ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, value).getOrThrow());
            configured.add(id.toString(), row);
        }
        report.add("configuredFeatures", configured);
        var structures = new JsonObject();
        var structureRegistry = access.registryOrThrow(Registries.STRUCTURE);
        for (var id : new TreeSet<>(structureRegistry.keySet())) {
            var value = structureRegistry.get(id);
            var row = new JsonObject();
            row.addProperty("class", value.getClass().getName());
            row.add("value", Structure.DIRECT_CODEC.encodeStart(ops, value).getOrThrow());
            structures.add(id.toString(), row);
        }
        report.add("structures", structures);
        var sets = new JsonObject();
        var setRegistry = access.registryOrThrow(Registries.STRUCTURE_SET);
        for (var id : new TreeSet<>(setRegistry.keySet()))
            sets.add(id.toString(), StructureSet.DIRECT_CODEC.encodeStart(ops, setRegistry.get(id)).getOrThrow());
        report.add("structureSets", sets);
        var resources = new JsonObject();
        for (String prefix : List.of("worldgen", "neoforge/biome_modifier", "tags/worldgen"))
            server.getResourceManager().listResourceStacks(prefix, id -> id.getPath().endsWith(".json")).forEach((id, stack) ->
                resources.add(id.toString(), new Gson().toJsonTree(stack.stream().map(resource -> resource.sourcePackId()).toList())));
        if (resources.isEmpty()) throw new AssertionError("Resolved resource inventory is empty");
        report.add("resourceStacks", resources);
        if (level.getChunkSource().getGenerator() instanceof SkyIslandChunkGenerator generator) {
            var random = level.getChunkSource().randomState();
            var field = ((GeologicalGenerator)(Object)generator).createSky$field(random);
            var settings = generator.resolvedSkyIslandSettings();
            var ancientCity = structureRegistry.get(ResourceLocation.withDefaultNamespace("ancient_city"));
            var observations = new JsonArray();
            var anchors = field.collectIslandPreviewsInRadius(0, 0, 16000, settings).stream()
                    .filter(p -> p.family() == IslandField.IslandFamily.ANCHOR_PLATEAU)
                    .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).toList();
            for (var anchor : anchors) {
                int x = (anchor.x() >> 4) * 16 + 8, z = (anchor.z() >> 4) * 16 + 8;
                if (!generator.getBiomeSource().getNoiseBiome(x >> 2, -48 >> 2, z >> 2, random.sampler())
                        .getRegisteredName().equals("minecraft:deep_dark")) continue;
                var column = field.sampleColumnProfile(x, z, settings).terrainColumn();
                if (!column.exists()) continue;
                var row = new JsonObject();
                row.addProperty("x", x); row.addProperty("z", z);
                row.addProperty("bottom", column.bottomY()); row.addProperty("top", column.topY());
                int middle = (column.bottomY() + column.topY()) >> 1;
                int lower = column.bottomY() + column.thickness() / 4;
                try (var scope = SkyIslandDeepDarkBiomeProjector.activateForStructureGeneration(generator, random, null, ancientCity)) {
                    row.addProperty("midpointBiome", generator.getBiomeSource().getNoiseBiome(x >> 2, middle >> 2, z >> 2, random.sampler()).getRegisteredName());
                    row.addProperty("lowerQuarterBiome", generator.getBiomeSource().getNoiseBiome(x >> 2, lower >> 2, z >> 2, random.sampler()).getRegisteredName());
                }
                var context = new org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.PlacementRequest.StructureGenerationContext(
                        generator, generator.getBiomeSource(), access, random, server.getStructureManager(), 0);
                var validity = org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.StructureGenerationAttemptService
                        .biomeValidityAt(ancientCity, context, new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4), middle);
                row.addProperty("precheckValid", validity.valid());
                row.addProperty("precheckBiome", validity.biomeId());
                if (observations.size() < 2) {
                    var chunkPos = new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4);
                    var chunk = level.getChunk(chunkPos.x, chunkPos.z, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
                    var request = new org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.PlacementRequest(
                            ancientCity, ResourceLocation.withDefaultNamespace("ancient_city"),
                            net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START, null,
                            level.structureManager(), net.minecraft.core.SectionPos.of(chunkPos, level.getMinSection()), chunk,
                            chunkPos, settings, field, level.getSeed(), null, null, level.dimension(), context);
                    var decision = new org.sathrek.sky_archipelago.worldgen.generator.structure.v2.underground.UndergroundV2PlacementEngine().place(request);
                    row.addProperty("placementAccepted", decision.accepted());
                    row.addProperty("placementStage", decision.stage());
                    row.addProperty("placementDetails", decision.details());
                    if (decision.accepted()) {
                        row.addProperty("pieces", decision.structureStart().getPieces().size());
                        row.addProperty("bounds", decision.structureStart().getBoundingBox().toString());
                    }
                }
                observations.add(row);
                if (observations.size() >= 12) break;
            }
            report.add("ancientCityEligibility", observations);
            var controls = new JsonArray();
            var context = new org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.PlacementRequest.StructureGenerationContext(
                    generator, generator.getBiomeSource(), access, random, server.getStructureManager(), 0);
            for (var anchor : anchors.subList(0, Math.min(24, anchors.size()))) {
                int x = (anchor.x() >> 4) * 16 + 8, z = (anchor.z() >> 4) * 16 + 8;
                var column = field.sampleColumnProfile(x, z, settings).terrainColumn();
                if (!column.exists()) continue;
                int middle = (column.bottomY() + column.topY()) >> 1;
                int lower = column.bottomY() + column.thickness() / 4;
                String expected;
                try (var scope = SkyIslandDeepDarkBiomeProjector.activateForStructureGeneration(generator, random, null, ancientCity)) {
                    expected = generator.getBiomeSource().getNoiseBiome(x >> 2, lower >> 2, z >> 2, random.sampler()).getRegisteredName();
                }
                var chunkPos = new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4);
                var checked = org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.StructureGenerationAttemptService
                        .biomeValidityAt(ancientCity, context, chunkPos, middle);
                if (design.inherent.createskycompat.terrain.AncientCityBiomeScope.active())
                    throw new AssertionError("Ancient-city query scope leaked");
                var row = new JsonObject();
                row.addProperty("expectedBiome", expected);
                row.addProperty("precheckBiome", checked.biomeId());
                row.addProperty("matchesProjectedBand", expected.equals(checked.biomeId()));
                var trial = structureRegistry.get(ResourceLocation.withDefaultNamespace("trial_chambers"));
                String trialExpected;
                try (var scope = SkyIslandDeepDarkBiomeProjector.activateForStructureGeneration(generator, random, null, trial)) {
                    trialExpected = generator.getBiomeSource().getNoiseBiome(x >> 2, middle >> 2, z >> 2, random.sampler()).getRegisteredName();
                }
                var trialChecked = org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.StructureGenerationAttemptService
                        .biomeValidityAt(trial, context, chunkPos, middle);
                if (!trialExpected.equals(trialChecked.biomeId())) throw new AssertionError("Trial-chamber eligibility changed");
                row.addProperty("trialChamberUnchanged", true);
                controls.add(row);
            }
            report.add("biomeControls", controls);
            var ordinary = (SkyIslandChunkGenerator)access.registryOrThrow(Registries.WORLD_PRESET)
                    .get(ResourceLocation.parse("sky_archipelago:sky_islands")).createWorldDimensions().overworld();
            var ordinaryRandom = net.minecraft.world.level.levelgen.RandomState.create(ordinary.generatorSettings().value(),
                    access.lookupOrThrow(Registries.NOISE), level.getSeed());
            var ordinaryContext = new org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.PlacementRequest.StructureGenerationContext(
                    ordinary, ordinary.getBiomeSource(), access, ordinaryRandom, server.getStructureManager(), 0);
            var ordinaryChunk = new net.minecraft.world.level.ChunkPos(0, 0);
            String ordinaryExpected;
            try (var scope = SkyIslandDeepDarkBiomeProjector.activateForStructureGeneration(ordinary, ordinaryRandom, null, ancientCity)) {
                ordinaryExpected = ordinary.getBiomeSource().getNoiseBiome(2, 16, 2, ordinaryRandom.sampler()).getRegisteredName();
            }
            var ordinaryChecked = org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.StructureGenerationAttemptService
                    .biomeValidityAt(ancientCity, ordinaryContext, ordinaryChunk, 64);
            if (!ordinaryExpected.equals(ordinaryChecked.biomeId())) throw new AssertionError("Ordinary preset eligibility changed");
            report.addProperty("ordinaryPresetUnchanged", true);
            report.add("locatePreviewControls", locateControls(server, generator, ordinary, ordinaryRandom));
        }
        report.addProperty("complete", true);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }

    private static JsonObject locateControls(MinecraftServer server, SkyIslandChunkGenerator generator,
            SkyIslandChunkGenerator ordinary, net.minecraft.world.level.levelgen.RandomState ordinaryRandom) throws Exception {
        var level = server.overworld();
        var access = server.registryAccess();
        var registry = access.registryOrThrow(Registries.STRUCTURE);
        var method = net.minecraft.world.level.levelgen.structure.StructureCheck.class
                .getDeclaredMethod("canCreateStructure", net.minecraft.world.level.ChunkPos.class, Structure.class);
        method.setAccessible(true);
        var checker = new net.minecraft.world.level.levelgen.structure.StructureCheck(null, access,
                server.getStructureManager(), level.dimension(), generator, level.getChunkSource().randomState(),
                level, generator.getBiomeSource(), level.getSeed(), net.minecraft.util.datafix.DataFixers.getDataFixer());
        var ordinaryChecker = new net.minecraft.world.level.levelgen.structure.StructureCheck(null, access,
                server.getStructureManager(), level.dimension(), ordinary, ordinaryRandom,
                level, ordinary.getBiomeSource(), level.getSeed(), net.minecraft.util.datafix.DataFixers.getDataFixer());
        var chunk = new net.minecraft.world.level.ChunkPos(0, 0);
        var rows = new JsonObject();
        for (String name : List.of("ancient_city", "mineshaft", "monument", "ocean_ruin_cold", "ruined_portal_ocean")) {
            var id = ResourceLocation.withDefaultNamespace(name);
            var structure = registry.get(id);
            boolean preview = (boolean)method.invoke(checker, chunk, structure);
            boolean expected = !generator.resolvedSkyIslandSettings().advanced().structurePlacementPolicy()
                    .denylistedStructures().contains(id)
                    && generator.resolvedSkyIslandSettings().advanced().structurePlacementPolicy().categoryFor(id)
                       != org.sathrek.sky_archipelago.worldgen.structure.StructurePlacementCategory.WATER;
            if (preview != expected) throw new AssertionError("Relocated/ocean preview mismatch: " + id);
            boolean ordinaryActual = (boolean)method.invoke(ordinaryChecker, chunk, structure);
            boolean ordinaryExpected = structure.findValidGenerationPoint(new Structure.GenerationContext(access,
                    ordinary, ordinary.getBiomeSource(), ordinaryRandom, server.getStructureManager(), level.getSeed(),
                    chunk, level, structure.biomes()::contains)).isPresent();
            if (ordinaryActual != ordinaryExpected) throw new AssertionError("Ordinary locate preview changed: " + id);
            var row = new JsonObject(); row.addProperty("adaptedPreview", preview);
            row.addProperty("ordinaryUnchanged", true); rows.add(id.toString(), row);
        }
        var ancientCity = registry.get(ResourceLocation.withDefaultNamespace("ancient_city"));
        checker.onStructureLoad(chunk, Map.of());
        var placement = level.getChunkSource().getGeneratorState().getPlacementsForStructure(registry.wrapAsHolder(ancientCity)).getFirst();
        if (checker.checkStart(chunk, ancientCity, placement, false)
                != net.minecraft.world.level.levelgen.structure.StructureCheckResult.START_NOT_PRESENT)
            throw new AssertionError("Known empty chunk was bypassed");
        rows.addProperty("knownEmptyChunkRetained", true);
        return rows;
    }

}

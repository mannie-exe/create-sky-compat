package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.nio.file.*;
import java.util.*;

/** Natural candidate starts, followed by bounded realization of progression-critical content. */
final class StructureCoverageProbe {
    private StructureCoverageProbe() {}
    private static BlockPos lootPosition;
    private static String lootTable;
    private static Path pendingOutput;

    static void run(MinecraftServer server, Path output) throws Exception {
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var state = level.getChunkSource().getGeneratorState();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(level.getChunkSource().randomState());
        var settings = generator.resolvedSkyIslandSettings();
        var registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var anchors = field.collectIslandPreviewsInRadius(0, 0, 16000, settings).stream()
                .filter(p -> p.family() == IslandField.IslandFamily.ANCHOR_PLATEAU)
                .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).limit(16).toList();
        var report = new JsonObject(); report.addProperty("seed", level.getSeed());
        var rows = new JsonArray();
        var names = Boolean.getBoolean("neighbors.probe.surfaceStructures")
                ? List.of("village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga",
                        "pillager_outpost", "mansion", "desert_pyramid", "jungle_pyramid", "igloo", "swamp_hut",
                        "trail_ruins", "ruined_portal", "ruined_portal_mountain", "buried_treasure",
                        "shipwreck", "shipwreck_beached", "monument", "ocean_ruin_cold", "ocean_ruin_warm")
                : List.of("stronghold", "trial_chambers", "ancient_city", "mineshaft", "mineshaft_mesa");
        for (String name : names) {
            var id = ResourceLocation.withDefaultNamespace(name);
            var holder = registry.getHolder(id).orElseThrow();
            var candidates = new LinkedHashSet<ChunkPos>();
            for (var placement : state.getPlacementsForStructure(holder)) {
                if (placement instanceof ConcentricRingsStructurePlacement ring) {
                    var positions = state.getRingPositionsFor(ring);
                    if (positions != null) positions.stream()
                            .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x, p.z)))
                            .limit(12).forEach(candidates::add);
                } else if (placement instanceof RandomSpreadStructurePlacement spread) {
                    for (var anchor : anchors) {
                        int cx = anchor.x() >> 4, cz = anchor.z() >> 4;
                        if (name.startsWith("mineshaft") || name.equals("buried_treasure")) {
                            // Mineshaft spacing is one chunk with a frequency gate.
                            // Examine descriptor-local candidates without generating their neighbors.
                            int selected = 0;
                            search: for (int radius = 0; radius <= 10; radius++)
                                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                                    if (spread.isStructureChunk(state, cx + dx, cz + dz)) {
                                        candidates.add(new ChunkPos(cx + dx, cz + dz));
                                        if (++selected >= 2) break search;
                                    }
                                }
                        } else {
                            var potential = spread.getPotentialStructureChunk(level.getSeed(), cx, cz);
                            if (spread.isStructureChunk(state, potential.x, potential.z)) candidates.add(potential);
                        }
                    }
                }
            }
            var row = new JsonObject(); row.addProperty("id", id.toString());
            row.addProperty("category", settings.advanced().structurePlacementPolicy().categoryFor(id).name());
            var samples = new JsonArray(); int accepted = 0, realized = 0;
            for (var candidate : candidates.stream().limit(16).toList()) {
                var chunk = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS);
                var start = chunk.getStartForStructure(holder.value());
                var sample = new JsonObject(); sample.addProperty("chunkX", candidate.x); sample.addProperty("chunkZ", candidate.z);
                boolean valid = start != null && start.isValid(); sample.addProperty("valid", valid);
                if (valid) {
                    accepted++;
                    var bounds = start.getBoundingBox();
                    sample.addProperty("bounds", bounds.toString()); sample.addProperty("pieces", start.getPieces().size());
                    sample.addProperty("portalRoomPieces", start.getPieces().stream()
                            .filter(piece -> piece.getClass().getSimpleName().equals("PortalRoom")).count());
                    int footprintChunks = ((bounds.maxX() >> 4) - (bounds.minX() >> 4) + 1)
                            * ((bounds.maxZ() >> 4) - (bounds.minZ() >> 4) + 1);
                    sample.addProperty("footprintChunks", footprintChunks);
                    if (realized == 0 && footprintChunks <= 400) {
                        var blocks = new TreeMap<String, Integer>();
                        var pos = new BlockPos.MutableBlockPos();
                        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++)
                            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                                var full = level.getChunk(cx, cz);
                                for (int x = Math.max(cx * 16, bounds.minX()); x <= Math.min(cx * 16 + 15, bounds.maxX()); x++)
                                    for (int z = Math.max(cz * 16, bounds.minZ()); z <= Math.min(cz * 16 + 15, bounds.maxZ()); z++)
                                        for (int y = Math.max(level.getMinBuildHeight(), bounds.minY()); y <= Math.min(level.getMaxBuildHeight() - 1, bounds.maxY()); y++) {
                                            var block = full.getBlockState(pos.set(x, y, z)).getBlock();
                                            if (block != net.minecraft.world.level.block.Blocks.AIR
                                                    && block != net.minecraft.world.level.block.Blocks.CAVE_AIR)
                                                blocks.merge(BuiltInRegistries.BLOCK.getKey(block).toString(), 1, Integer::sum);
                                        }
                            }
                        sample.add("blocks", new Gson().toJsonTree(blocks));
                        if (!name.startsWith("mineshaft")) {
                            var containers = VillageContentProbe.inspect(level, bounds,
                                    !Boolean.getBoolean("neighbors.probe.surfaceStructures"));
                            sample.add("containers", containers);
                            if (lootPosition == null && !containers.isEmpty()) {
                                var first = containers.get(0).getAsJsonObject();
                                lootPosition = new Gson().fromJson(first.get("position"), BlockPos.class);
                                lootTable = first.get("lootTable").getAsString();
                            }
                        }
                        if (name.startsWith("mineshaft")) sample.addProperty("chestMinecarts", level.getEntitiesOfClass(
                                net.minecraft.world.entity.vehicle.MinecartChest.class, net.minecraft.world.phys.AABB.of(bounds)).size());
                        long locateBegan = System.nanoTime();
                        var located = generator.findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder),
                                candidate.getMiddleBlockPosition(0), 2, false);
                        sample.addProperty("locateMillis", (System.nanoTime() - locateBegan) / 1_000_000.0);
                        sample.addProperty("locateFound", located != null && located.getSecond().equals(holder));
                        if (located != null) sample.addProperty("locatePosition", located.getFirst().toShortString());
                        var placement = state.getPlacementsForStructure(holder).getFirst();
                        sample.addProperty("sourcePresenceBeforeFull", level.structureManager()
                                .checkStructurePresence(candidate, holder.value(), placement, false).name());
                        sample.addProperty("canonicalChunk", start.getChunkPos().toString());
                        var sourceFull = level.getChunk(candidate.x, candidate.z);
                        var sourceStart = sourceFull.getStartForStructure(holder.value());
                        sample.addProperty("sourceStartAfterFull", sourceStart != null && sourceStart.isValid());
                        sample.addProperty("sourcePresenceAfterFull", level.structureManager()
                                .checkStructurePresence(candidate, holder.value(), placement, false).name());
                        var afterFull = generator.findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder),
                                candidate.getMiddleBlockPosition(0), 2, false);
                        sample.addProperty("locateAfterSourceFull", afterFull != null);
                        if (afterFull != null) sample.addProperty("locateAfterFullPosition", afterFull.getFirst().toShortString());
                        if (name.equals("stronghold") && blocks.getOrDefault("minecraft:end_portal_frame", 0) < 12)
                            throw new AssertionError("Accepted stronghold has no complete portal frame: " + bounds);
                        if (name.equals("trial_chambers") && blocks.getOrDefault("minecraft:trial_spawner", 0) == 0)
                            throw new AssertionError("Accepted trial chamber has no realized trial spawner: " + bounds);
                        if (name.equals("ancient_city") && blocks.getOrDefault("minecraft:reinforced_deepslate", 0) == 0)
                            throw new AssertionError("Accepted ancient city has no realized center: " + bounds);
                        realized++;
                    }
                }
                samples.add(sample);
                System.out.println("COVERAGE_STRUCTURE " + name + " " + candidate + " valid=" + valid);
                if (accepted >= 2) break;
            }
            row.add("candidates", samples); row.addProperty("accepted", accepted); row.addProperty("realized", realized); rows.add(row);
            report.add("structures", rows); report.addProperty("complete", false);
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
        if (lootPosition != null && net.neoforged.fml.ModList.get().isLoaded("lootr")) {
            var center = new ChunkPos(lootPosition);
            // Lootr waits for a loaded 5x5 neighborhood and subsequent server ticks.
            for (int x = center.x - 2; x <= center.x + 2; x++) for (int z = center.z - 2; z <= center.z + 2; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
            pendingOutput = output;
        }
        report.addProperty("complete", pendingOutput == null);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }

    static void finish(MinecraftServer server) throws Exception {
        if (pendingOutput == null) return;
        var level = server.overworld();
        var entity = level.getBlockEntity(lootPosition);
        var report = JsonParser.parseString(Files.readString(pendingOutput)).getAsJsonObject();
        var conversion = new JsonObject();
        String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(lootPosition).getBlock()).toString();
        conversion.addProperty("block", block);
        conversion.addProperty("originalLootTable", lootTable);
        boolean converted = block.startsWith("lootr:");
        conversion.addProperty("converted", converted);
        if (converted) {
            var key = (net.minecraft.resources.ResourceKey<?>)entity.getClass().getMethod("getInfoLootTable").invoke(entity);
            conversion.addProperty("convertedLootTable", key.location().toString());
            if (!key.location().toString().equals(lootTable)) throw new AssertionError("Lootr changed loot table");
        }
        report.add("lootrConversion", conversion);
        report.addProperty("complete", converted);
        Files.writeString(pendingOutput, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        pendingOutput = null;
        if (!converted) throw new AssertionError("Generated container did not convert after loaded-neighborhood ticks");
    }

}

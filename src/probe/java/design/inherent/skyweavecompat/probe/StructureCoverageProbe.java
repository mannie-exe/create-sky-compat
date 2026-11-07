package design.inherent.skyweavecompat.probe;

import org.sathrek.sky_archipelago.worldgen.generator.field.model.IslandFamily;
import com.google.gson.*;
import design.inherent.skyweavecompat.terrain.GeologicalGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
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
        var field = ((GeologicalGenerator)(Object)generator).skyweave$field(level.getChunkSource().randomState());
        var settings = generator.resolvedSkyIslandSettings();
        var registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var anchors = field.collectIslandPreviewsInRadius(0, 0, 16000, settings).stream()
                .filter(p -> p.family() == IslandFamily.ANCHOR_PLATEAU)
                .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).limit(16).toList();
        var report = new JsonObject(); report.addProperty("seed", level.getSeed());
        var rows = new JsonArray();
        var names = Boolean.getBoolean("neighbors.probe.surfaceStructures")
                ? List.of("village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga",
                        "pillager_outpost", "mansion", "desert_pyramid", "jungle_pyramid", "igloo", "swamp_hut",
                        "trail_ruins", "ruined_portal", "ruined_portal_mountain", "buried_treasure",
                        "shipwreck", "shipwreck_beached", "monument", "ocean_ruin_cold", "ocean_ruin_warm")
                : List.of("stronghold", "trial_chambers", "ancient_city", "mineshaft", "mineshaft_mesa");
        String selectedId = System.getProperty("neighbors.probe.structureId", "");
        if (!selectedId.isEmpty()) {
            if (!names.contains(selectedId)) throw new IllegalArgumentException("Structure is outside this fixture: " + selectedId);
            names = List.of(selectedId);
        }
        report.addProperty("layoutsOnly", Boolean.getBoolean("neighbors.probe.layoutsOnly"));
        for (String name : names) {
            var id = ResourceLocation.withDefaultNamespace(name);
            var holder = registry.getHolder(id).orElseThrow();
            var candidates = new LinkedHashSet<ChunkPos>();
            var observedStarts = new LinkedHashSet<ChunkPos>();
            var nativeReport = org.sathrek.sky_archipelago.worldgen.benchmark.StructureBenchmarkRecorder
                    .report("skyweave", level.getSeed(), List.of(), Map.of());
            for (var observed : nativeReport.samples()) if (observed.accepted() && observed.structureId().equals(id.toString()))
                observedStarts.add(new ChunkPos(observed.chunkX(),observed.chunkZ()));
            String previous = System.getProperty("neighbors.probe.previousStructures", "");
            var replay = new LinkedHashSet<ChunkPos>();
            if (!previous.isEmpty() && Files.isRegularFile(Path.of(previous))) {
                var prior = JsonParser.parseString(Files.readString(Path.of(previous))).getAsJsonObject();
                for (var row : prior.getAsJsonArray("structures")) if (row.getAsJsonObject().get("id").getAsString().equals(id.toString()))
                    for (var old : row.getAsJsonObject().getAsJsonArray("candidates")) {
                        var sample = old.getAsJsonObject();
                        replay.add(new ChunkPos(sample.get("chunkX").getAsInt(),sample.get("chunkZ").getAsInt()));
                        if (sample.get("valid").getAsBoolean()) observedStarts.add(new ChunkPos(sample.get("chunkX").getAsInt(),sample.get("chunkZ").getAsInt()));
                    }
            }
            // Realizing a footprint discovers additional nearby starts. Reopening
            // must replay the original candidates, including rejected ones, rather
            // than let those newly observed starts displace the tested sample.
            candidates.addAll(replay.isEmpty() ? observedStarts : replay);
            for (var placement : replay.isEmpty() ? state.getPlacementsForStructure(holder) : List.<StructurePlacement>of()) {
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
                        } else if (settings.advanced().structurePlacementPolicy().categoryFor(id)
                                == org.sathrek.sky_archipelago.worldgen.structure.StructurePlacementCategory.WATER) {
                            // A sparse island can straddle a placement-region boundary. Survey
                            // actual neighboring-region candidates in usable water, not only the
                            // region containing the descriptor's center. This is not a frequency sample.
                            var local = new ArrayList<ChunkPos>();
                            for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++) {
                                var potential = spread.getPotentialStructureChunk(level.getSeed(),cx+dx*spread.spacing(),cz+dz*spread.spacing());
                                if (spread.isStructureChunk(state,potential.x,potential.z)
                                        && field.sampleWaterArchetypeColumnProfile(potential.getMiddleBlockX(),potential.getMiddleBlockZ(),settings).water())
                                    local.add(potential);
                            }
                            local.sort(Comparator.comparingDouble(p -> Math.hypot(p.getMiddleBlockX()-anchor.x(),p.getMiddleBlockZ()-anchor.z())));
                            candidates.addAll(local);
                        } else {
                            var potential = spread.getPotentialStructureChunk(level.getSeed(), cx, cz);
                            if (spread.isStructureChunk(state, potential.x, potential.z)) candidates.add(potential);
                        }
                    }
                }
            }
            var row = new JsonObject(); row.addProperty("id", id.toString());
            row.addProperty("category", settings.advanced().structurePlacementPolicy().categoryFor(id).name());
            if (settings.advanced().structurePlacementPolicy().categoryFor(id)
                    == org.sathrek.sky_archipelago.worldgen.structure.StructurePlacementCategory.WATER)
                row.addProperty("candidateSelection","Wet-center neighboring natural placement regions; not a frequency estimate");
            var samples = new JsonArray(); int accepted = 0, realized = 0;
            for (var candidate : candidates.stream().limit(Math.clamp(Integer.getInteger("neighbors.probe.samples", 12),1,16)).toList()) {
                var chunk = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS);
                var start = chunk.getStartForStructure(holder.value());
                var sample = new JsonObject(); sample.addProperty("chunkX", candidate.x); sample.addProperty("chunkZ", candidate.z);
                boolean valid = start != null && start.isValid(); sample.addProperty("valid", valid);
                sample.addProperty("source", observedStarts.contains(candidate) ? "startup-native-start" : "natural-candidate");
                if (valid) {
                    accepted++;
                    var bounds = start.getBoundingBox();
                    sample.addProperty("bounds", bounds.toString()); sample.addProperty("pieces", start.getPieces().size());
                    var serialization = net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
                    sample.addProperty("piecesBeforeRealizationDigest", piecesDigest(start, serialization));
                    if (name.equals("ancient_city")) {
                        var root = start.getPieces().getFirst().getBoundingBox();
                        sample.addProperty("rootBounds",root.toString());
                        var host = field.sampleDominantIslandDescriptor((root.minX()+root.maxX())>>1, (root.minZ()+root.maxZ())>>1, settings);
                        if (host == null) throw new AssertionError("Accepted city core has no host");
                        sample.addProperty("hostInterior", design.inherent.skyweavecompat.terrain.IslandInteriors.kind(
                                host,design.inherent.skyweavecompat.terrain.InteriorConfiguration.profile()).name());
                        sample.add("host",new Gson().toJsonTree(host));
                        var plan = design.inherent.skyweavecompat.terrain.city.StructureBurial.plan(start.getPieces().stream().map(piece -> {
                            var b=piece.getBoundingBox();
                            return new design.inherent.skyweavecompat.terrain.city.StructureBurial.Box(b.minX(),b.minY(),b.minZ(),b.maxX()+1,b.maxY()+1,b.maxZ()+1);
                        }).toList());
                        var body = (design.inherent.skyweavecompat.terrain.InteriorField)(Object)field;
                        var cached = new HashMap<Long,org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn>();
                        double burial = plan.buriedFraction(0,(x,y,z)->cached.computeIfAbsent((long)x<<32^(z&0xffffffffL),
                                ignored -> body.skyweave$exteriorOf(host,x,z,settings)).contains(y));
                        sample.addProperty("savedStartBuriedFraction",burial);
                        if (burial + 1e-12 < design.inherent.skyweavecompat.terrain.city.CityConfiguration.policy().minimumBuriedFraction())
                            throw new AssertionError("Committed city no longer meets burial policy");
                    }
                    sample.addProperty("portalRoomPieces", start.getPieces().stream()
                            .filter(piece -> piece.getClass().getSimpleName().equals("PortalRoom")).count());
                    int footprintChunks = ((bounds.maxX() >> 4) - (bounds.minX() >> 4) + 1)
                            * ((bounds.maxZ() >> 4) - (bounds.minZ() >> 4) + 1);
                    sample.addProperty("footprintChunks", footprintChunks);
                    if (!Boolean.getBoolean("neighbors.probe.layoutsOnly") && realized == 0 && footprintChunks <= 400) {
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
                            if (lootPosition == null) for (var container : containers) {
                                var first = container.getAsJsonObject();
                                // Dispensers and trial vaults retain native loot;
                                // Lootr conversion is only expected for its supported storage.
                                if (!Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel")
                                        .contains(first.get("container").getAsString())) continue;
                                lootPosition = new Gson().fromJson(first.get("position"), BlockPos.class);
                                lootTable = first.get("lootTable").getAsString();
                                break;
                            }
                        }
                        if (name.startsWith("mineshaft")) sample.addProperty("chestMinecarts", level.getEntitiesOfClass(
                                net.minecraft.world.entity.vehicle.MinecartChest.class, net.minecraft.world.phys.AABB.of(bounds)).size());
                        if (name.startsWith("village_")) {
                            var skyVillage = registry.getHolder(ResourceLocation.parse("skyvillages:skyvillage"));
                            var nearby = new JsonArray();
                            if (skyVillage.isPresent()) for (var villagePlacement : state.getPlacementsForStructure(skyVillage.get())) {
                                if (!(villagePlacement instanceof design.inherent.skyweavecompat.placement.ArchipelagoVillagePlacement adapted)) continue;
                                var owner = adapted.owner((bounds.minX()+bounds.maxX())>>1,(bounds.minZ()+bounds.maxZ())>>1);
                                if (owner == null) continue;
                                for (var site : adapted.sites(owner)) {
                                    var neighbor = level.getChunk(site.chunkX(),site.chunkZ(),ChunkStatus.STRUCTURE_STARTS)
                                            .getStartForStructure(skyVillage.get().value());
                                    if (neighbor == null || !neighbor.isValid()) continue;
                                    var observed = new JsonObject();
                                    observed.add("owner",new Gson().toJsonTree(owner));
                                    observed.addProperty("chunkX",site.chunkX()); observed.addProperty("chunkZ",site.chunkZ());
                                    observed.addProperty("bounds",neighbor.getBoundingBox().toString());
                                    observed.addProperty("pieces",neighbor.getPieces().size());
                                    nearby.add(observed);
                                }
                            }
                            sample.add("sameArchipelagoSkyVillages",nearby);
                        }
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
                        if (name.equals("ancient_city")) {
                            if (blocks.getOrDefault("minecraft:reinforced_deepslate", 0) == 0)
                                throw new AssertionError("Accepted ancient city has no realized center: " + bounds);
                            var root = start.getPieces().getFirst().getBoundingBox();
                            int feet = 0, roofs = 0;
                            for (int x : new int[]{root.minX(),(root.minX()+root.maxX())>>1,root.maxX()})
                                for (int z : new int[]{root.minZ(),(root.minZ()+root.maxZ())>>1,root.maxZ()}) {
                                    boolean footing = false;
                                    for (int y = root.minY()-12; y <= root.minY(); y++) {
                                        pos.set(x,y,z);
                                        footing |= level.getBlockState(pos).isFaceSturdy(level,pos,net.minecraft.core.Direction.UP);
                                    }
                                    if (footing) feet++;
                                    int run = 0;
                                    for (int y = root.maxY()+1; y < level.getMaxBuildHeight(); y++) {
                                        pos.set(x,y,z);
                                        run = level.getBlockState(pos).isSolidRender(level,pos) ? run+1 : 0;
                                        if (run >= 8) { roofs++; break; }
                                    }
                                }
                            sample.addProperty("completedCoreFootings",feet); sample.addProperty("completedCoreRoofs",roofs);
                            if (feet != 9 || roofs != 9) throw new AssertionError("Completed city core lost floor or roof support: " + feet + "/" + roofs);
                            // Inspect actual room independently of template-block/burial success.
                            // Compare to the pre-structure solid intervals, not a clearance helper.
                            int roomSamples = 0, passable = 0, originallySolid = 0, opened = 0;
                            BlockPos witness = null;
                            for (int x = root.minX() + 2; x <= root.maxX() - 2; x += 2)
                                for (int z = root.minZ() + 2; z <= root.maxZ() - 2; z += 2) {
                                    var original = field.sampleSolidSegments(x, z, settings);
                                    for (int y = root.minY() + 3; y < root.maxY() - 1; y += 2) {
                                        final int height = y;
                                        boolean solid = original.stream().anyMatch(segment -> segment.contains(height));
                                        pos.set(x, y, z); roomSamples++;
                                        boolean free = level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
                                        if (solid) originallySolid++;
                                        if (free) { passable++; if (solid) opened++; if (witness == null) witness = pos.immutable(); }
                                    }
                                }
                            var room = new JsonObject();
                            room.addProperty("sampled",roomSamples); room.addProperty("twoHighAir",passable);
                            room.addProperty("preStructureSolid",originallySolid); room.addProperty("openedSolid",opened);
                            if (witness != null) room.add("airWitness",new Gson().toJsonTree(witness));
                            sample.add("completedCoreRoom",room);
                            if (passable < roomSamples * .35 || originallySolid > roomSamples * .5 && opened < originallySolid * .35)
                                throw new AssertionError("City core remains embedded: " + room);
                        }
                        realized++;
                    }
                    // Vanilla pieces persist placement flags for chests and spawners.
                    // Compare the finished state across save/reopen, not a pre-placement
                    // start against a post-placement save. Keep the earlier digest as evidence.
                    sample.addProperty("piecesDigest", piecesDigest(start, serialization));
                }
                samples.add(sample);
                System.out.println("COVERAGE_STRUCTURE " + name + " " + candidate + " valid=" + valid);
                if (accepted >= 2) break;
            }
            row.add("candidates", samples); row.addProperty("accepted", accepted); row.addProperty("realized", realized); rows.add(row);
            report.add("structures", rows); report.addProperty("complete", false);
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
        report.add("nativePlacement", new Gson().toJsonTree(
                org.sathrek.sky_archipelago.worldgen.benchmark.StructureBenchmarkRecorder.report("skyweave", level.getSeed(), List.of(), Map.of())));
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

    private static String piecesDigest(net.minecraft.world.level.levelgen.structure.StructureStart start,
                                      net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context)
            throws java.security.NoSuchAlgorithmException {
        String pieces = start.getPieces().stream().map(piece -> canonical(piece.createTag(context)))
                .collect(java.util.stream.Collectors.joining("\n"));
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(pieces.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static String canonical(net.minecraft.nbt.Tag tag) {
        if (tag instanceof net.minecraft.nbt.CompoundTag compound)
            return compound.getAllKeys().stream().sorted().map(key -> key + ":" + canonical(compound.get(key)))
                    .collect(java.util.stream.Collectors.joining(",","{","}"));
        if (tag instanceof net.minecraft.nbt.ListTag list)
            return list.stream().map(StructureCoverageProbe::canonical).collect(java.util.stream.Collectors.joining(",","[","]"));
        return tag.toString();
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

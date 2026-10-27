package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobCategory;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import java.nio.file.*;
import java.util.*;

/** Actual material checks and optional native tick observations in an isolated loaded world. */
final class WaterProbe {
    private record Site(WaterRegion region, BlockPos outlet) {}
    private static final List<Site> SITES = new ArrayList<>();
    private static JsonObject report;
    private static Path destination;
    private static int remaining, elapsed;
    private static long began;
    private WaterProbe() {}

    static boolean start(MinecraftServer server, Path output, int ticks) throws Exception {
        began = System.nanoTime(); destination = output;
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(level.getChunkSource().randomState());
        var settings = generator.resolvedSkyIslandSettings();
        var waterField = (InteriorField)(Object)field;
        var selected = new EnumMap<WaterRegion.Kind, WaterRegion>(WaterRegion.Kind.class);
        var admitted = new EnumMap<WaterRegion.Kind, Integer>(WaterRegion.Kind.class);
        boolean oceans = Boolean.getBoolean("neighbors.probe.oceans");
        int candidates = 0, descriptors = 0;
        boolean surveyComplete = true;
        search: for (int cx = -10; cx <= 10; cx++) for (int cz = -10; cz <= 10; cz++) {
            var members = ((ClusterField)(Object)field).createSky$members(cx, cz, settings);
            if (members.isEmpty()) continue;
            candidates++;
            for (var island : oceans ? members : List.of(members.getFirst())) {
                descriptors++;
                var region = waterField.createSky$waters().region(island, settings);
                if (region != null && region.ocean() == oceans) {
                    selected.putIfAbsent(region.kind(), region);
                    admitted.merge(region.kind(), 1, Integer::sum);
                }
            }
            if (oceans && ticks > 0 && selected.size() == 3) { surveyComplete = false; break search; }
        }
        report = new JsonObject(); report.addProperty("seed", Long.toString(level.getSeed()));
        report.addProperty("evidence", "targeted native water regions; not an unbiased occurrence survey");
        report.add("waterProfile", new Gson().toJsonTree(WaterConfiguration.profile()));
        report.add("oceanProfile", new Gson().toJsonTree(OceanConfiguration.profile()));
        report.addProperty("candidateAnchors", candidates); report.addProperty("requestedTicks", ticks);
        report.addProperty("candidateDescriptors", descriptors); report.addProperty("admissionSurveyComplete", surveyComplete);
        report.addProperty("surveyCellRadius", 10);
        report.add("admittedRegionCounts", new Gson().toJsonTree(admitted));
        report.add("regions", new Gson().toJsonTree(selected.values()));
        var checks = new JsonArray();
        long compared = 0;
        for (var region : selected.values()) {
            var targets = new LinkedHashSet<ChunkPos>();
            targets.add(new ChunkPos(region.x() >> 4, region.z() >> 4));
            targets.add(new ChunkPos((region.x() + region.radius() - 1) >> 4, region.z() >> 4));
            BlockPos outlet = new BlockPos(region.x(), region.level(), region.z());
            if (region.kind() == WaterRegion.Kind.OUTLET) {
                outer: for (int x = region.x() - region.radius(); x <= region.x() + region.radius(); x++)
                    for (int z = region.z() - region.radius(); z <= region.z() + region.radius(); z++)
                        if (region.shaft(x, z)) { outlet = new BlockPos(x, region.level(), z); break outer; }
            } else if (region.kind() == WaterRegion.Kind.OVERFLOW || region.kind() == WaterRegion.Kind.PERIMETER) {
                for (int step = 0; step <= region.reach(); step++) {
                    int x = region.x() + (int)Math.round(Math.cos(region.heading()) * step);
                    int z = region.z() + (int)Math.round(Math.sin(region.heading()) * step);
                    if (waterField.createSky$waterAt(x, z, settings).contains(region.level()))
                        outlet = new BlockPos(x, region.level(), z);
                }
            }
            targets.add(new ChunkPos(outlet));
            long sources = 0;
            var pos = new BlockPos.MutableBlockPos();
            for (var location : targets) {
                var chunk = new ProtoChunk(location, UpgradeData.EMPTY, level,
                        server.registryAccess().registryOrThrow(Registries.BIOME), null);
                generator.fillFromNoise(Blender.empty(), level.getChunkSource().randomState(), level.structureManager(), chunk).join();
                for (int x = location.getMinBlockX(); x <= location.getMaxBlockX(); x++)
                    for (int z = location.getMinBlockZ(); z <= location.getMaxBlockZ(); z++) {
                        var expected = generator.getBaseColumn(x, z, level, level.getChunkSource().randomState());
                        int surface = level.getMinBuildHeight(), floor = surface;
                        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                            var state = chunk.getBlockState(pos.set(x, y, z));
                            if (state != expected.getBlock(y)) throw new AssertionError("Water fill/query mismatch at " + pos);
                            compared++;
                            if (state.is(Blocks.WATER)) sources++;
                            if (Heightmap.Types.WORLD_SURFACE_WG.isOpaque().test(state)) surface = y + 1;
                            if (Heightmap.Types.OCEAN_FLOOR_WG.isOpaque().test(state)) floor = y + 1;
                        }
                        if (generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState()) != surface
                                || generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState()) != floor)
                            throw new AssertionError("Water height predicate mismatch at " + x + "," + z);
                    }
            }
            var check = new JsonObject(); check.addProperty("kind", region.kind().name());
            check.addProperty("sampledSourceBlocks", sources); check.add("outlet", new Gson().toJsonTree(outlet));
            checks.add(check); SITES.add(new Site(region, outlet));
            if (sources == 0) throw new AssertionError("Selected water region has no generated source blocks");
            if (ticks > 0) {
                var forced = new LinkedHashSet<ChunkPos>();
                int reach = region.radius() + 24;
                for (int cx = (region.x() - reach) >> 4; cx <= (region.x() + reach) >> 4; cx++)
                    for (int cz = (region.z() - reach) >> 4; cz <= (region.z() + reach) >> 4; cz++) forced.add(new ChunkPos(cx, cz));
                if (region.kind() == WaterRegion.Kind.OVERFLOW) {
                    for (int step = 0; step <= region.reach(); step += 8) {
                        int x = region.x() + (int)Math.round(Math.cos(region.heading()) * step);
                        int z = region.z() + (int)Math.round(Math.sin(region.heading()) * step);
                        if (!waterField.createSky$waterAt(x, z, settings).contains(region.level())) continue;
                        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) forced.add(new ChunkPos((x >> 4) + dx, (z >> 4) + dz));
                    }
                }
                for (var location : forced) { level.setChunkForced(location.x, location.z, true); level.getChunk(location.x, location.z); }
                check.addProperty("forcedChunks", forced.size());
            }
        }
        report.add("materialChecks", checks); report.addProperty("comparedBlocks", compared);
        report.add("observations", new JsonArray());
        if (ticks == 0 || selected.size() < 3) { finish(selected.size() == 3); return false; }
        observe(level, 0); remaining = ticks;
        return true;
    }

    static void tick(MinecraftServer server) {
        if (remaining <= 0) return;
        remaining--; elapsed++;
        if (remaining != 0) return;
        try {
            observe(server.overworld(), elapsed);
            var observations = report.getAsJsonArray("observations");
            var finalSites = observations.get(observations.size() - 1).getAsJsonObject().getAsJsonArray("sites");
            boolean valid = true;
            for (var element : finalSites) {
                var site = element.getAsJsonObject();
                if (!site.get("kind").getAsString().equals("PERIMETER"))
                    valid &= site.get("floorHoles").getAsInt() == 0 && site.get("rimHoles").getAsInt() == 0;
                valid &= site.get("sourceAirGaps").getAsInt() == 0;
                if (site.has("storedHabitatBiome")) valid &= site.get("aquaticPlantBlocks").getAsInt() > 0
                        && site.get("aquaticSpawnEntries").getAsInt() > 0;
                if (!site.get("kind").getAsString().equals("ENCLOSED") && elapsed >= 100)
                    valid &= site.get("flowingOutletBlocks").getAsInt() > 0;
                if (site.get("kind").getAsString().equals("OUTLET") && elapsed >= 100)
                    valid &= site.get("deepFlowingOutletBlocks").getAsInt() > 0;
                if (site.has("beachColumns")) valid &= site.get("beachColumns").getAsInt() > 0;
            }
            report.addProperty("fluidAndContainmentChecksPassed", valid);
            finish(valid);
        } catch (Throwable failure) {
            failure.printStackTrace();
            report.addProperty("failure", failure.toString());
            try { finish(false); } catch (Exception error) { error.printStackTrace(); }
        } finally { server.halt(false); }
    }

    private static void observe(ServerLevel level, int tick) {
        var observation = new JsonObject(); observation.addProperty("tick", tick);
        var sites = new JsonArray(); var pos = new BlockPos.MutableBlockPos();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var field = (InteriorField)(Object)((GeologicalGenerator)(Object)generator).createSky$field(level.getChunkSource().randomState());
        for (var site : SITES) {
            var region = site.region(); var row = new JsonObject(); row.addProperty("kind", region.kind().name());
            BlockPos spawn = null;
            var center = new BlockPos(region.x(), region.level() - 3, region.z());
            row.addProperty("centerAboveState", level.getBlockState(center.above()).toString());
            row.addProperty("centerBelowState", level.getBlockState(center.below()).toString());
            search: for (int dx = -region.radius() / 2; dx <= region.radius() / 2; dx++)
                for (int dz = -region.radius() / 2; dz <= region.radius() / 2; dz++) {
                    var candidate = center.offset(dx, 0, dz);
                    if (region.source(candidate.getX(), candidate.getY(), candidate.getZ())
                            && field.createSky$waterAt(candidate.getX(), candidate.getZ(), generator.resolvedSkyIslandSettings()).contains(candidate.getY())
                            && level.getFluidState(candidate).is(FluidTags.WATER)
                            && level.getFluidState(candidate.below()).is(FluidTags.WATER)
                            && level.getBlockState(candidate.above()).is(Blocks.WATER)) {
                        spawn = candidate; break search;
                    }
                }
            if (spawn == null) throw new AssertionError("No unobstructed water-animal witness in " + region);
            boolean spawnAllowed = WaterAnimal.checkSurfaceWaterAnimalSpawnRules(EntityType.COD, level,
                    MobSpawnType.NATURAL, spawn, level.random);
            row.addProperty("waterAnimalHeightPredicate", spawnAllowed);
            if (region.ocean()) {
                var biome = level.getChunk(spawn).getNoiseBiome(spawn.getX() >> 2, spawn.getY() >> 2, spawn.getZ() >> 2);
                row.addProperty("storedHabitatBiome", biome.getRegisteredName());
                row.addProperty("aquaticSpawnEntries", biome.value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap().size()
                        + biome.value().getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap().size());
                if (!biome.is(BiomeTags.IS_OCEAN)) throw new AssertionError("Ocean habitat stored the wrong biome at " + spawn + ": " + biome);
            }
            if (!spawnAllowed) throw new AssertionError("Local-water spawn height rejected basin at " + spawn
                    + "; localSurface=" + LocalWater.spawnSurface(level, spawn, level.getSeaLevel()));
            if (LocalWater.spawnSurface(level, spawn.below(region.depth() + 16), level.getSeaLevel()) != level.getSeaLevel())
                throw new AssertionError("Local-water spawn height escaped its water region");
            var foreign = level.getServer().getLevel(Level.NETHER);
            if (foreign != null && LocalWater.spawnSurface(foreign, spawn, foreign.getSeaLevel()) != foreign.getSeaLevel())
                throw new AssertionError("Local-water spawn height changed a foreign generator");
            int lowest = region.level() + 1, fluid = 0, frozen = 0, flowing = 0, deepFlowing = 0;
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
                for (int y = level.getMinBuildHeight(); y <= region.level(); y++) {
                    var state = level.getBlockState(pos.set(site.outlet().getX() + dx, y, site.outlet().getZ() + dz));
                    if (state.is(Blocks.WATER)) { lowest = Math.min(lowest, y); fluid++; }
                    if (state.is(Blocks.WATER) && !state.getFluidState().isSource()) {
                        flowing++;
                        if (y < region.level() - 4) deepFlowing++;
                    }
                    if (state.is(Blocks.ICE)) frozen++;
                }
            row.addProperty("outletWaterBlocks", fluid); row.addProperty("lowestOutletWaterY", lowest);
            row.addProperty("flowingOutletBlocks", flowing); row.addProperty("iceBlocks", frozen);
            row.addProperty("deepFlowingOutletBlocks", deepFlowing);
            int floorHoles = 0, rimHoles = 0, sourceAirGaps = 0, aquaticPlants = 0, coral = 0, wetColumns = 0;
            var holes = new JsonArray();
            for (int x = region.x() - region.radius() - 1; x <= region.x() + region.radius() + 1; x++)
                for (int z = region.z() - region.radius() - 1; z <= region.z() + region.radius() + 1; z++) {
                    boolean plannedWater = false;
                    if (region.basin(x, z)) {
                        var planned = field.createSky$waterAt(x, z, generator.resolvedSkyIslandSettings());
                        plannedWater = !planned.sources().isEmpty();
                        if (plannedWater) wetColumns++;
                        for (int y = region.bed(x, z) + 1; y <= region.level(); y++) {
                            var state = level.getBlockState(pos.set(x, y, z));
                            if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS)
                                    || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.SEA_PICKLE)) aquaticPlants++;
                            if (state.is(BlockTags.CORALS) || state.is(BlockTags.CORAL_BLOCKS)) coral++;
                            if (planned.contains(y) && state.isAir()) {
                                sourceAirGaps++;
                                if (holes.size() < 12) holes.add(x + "," + y + "," + z + ": source replaced by air");
                            }
                        }
                    }
                    if (region.basin(x, z) && !region.shaft(x, z)
                            && (region.kind() != WaterRegion.Kind.PERIMETER || plannedWater)) {
                        int bed = region.bed(x, z);
                        if (region.channel(x, z)) bed = Math.min(bed, region.level() - 2);
                        var state = level.getBlockState(pos.set(x, bed, z));
                        if (!state.isCollisionShapeFullBlock(level, pos)) {
                            floorHoles++;
                            if (holes.size() < 12) holes.add(x + "," + bed + "," + z + ": " + state);
                        }
                    } else if (region.kind() != WaterRegion.Kind.PERIMETER && !region.channel(x, z) && !region.basin(x, z)) {
                        boolean neighbor = region.basin(x + 1, z) || region.basin(x - 1, z)
                                || region.basin(x, z + 1) || region.basin(x, z - 1);
                        if (neighbor) for (int y = region.level() - 1; y <= region.level(); y++) {
                            var state = level.getBlockState(pos.set(x, y, z));
                            if (!state.isCollisionShapeFullBlock(level, pos)) {
                                rimHoles++;
                                if (holes.size() < 12) holes.add(x + "," + y + "," + z + ": " + state);
                            }
                        }
                    }
                }
            row.addProperty("floorHoles", floorHoles); row.addProperty("rimHoles", rimHoles);
            row.addProperty("sourceAirGaps", sourceAirGaps);
            row.addProperty("aquaticPlantBlocks", aquaticPlants);
            row.addProperty("coralBlocks", coral); row.addProperty("plannedWaterColumns", wetColumns);
            if (region.ocean() && region.kind() == WaterRegion.Kind.OVERFLOW) {
                int beach = 0, sand = 0, reach = region.radius() + region.beachWidth();
                for (int x = region.x() - reach; x <= region.x() + reach; x++)
                    for (int z = region.z() - reach; z <= region.z() + reach; z++) {
                        if (region.basin(x, z) || !region.affects(x, z)) continue;
                        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                        var chunk = level.getChunk(x >> 4, z >> 4);
                        if (chunk.getNoiseBiome(x >> 2, y >> 2, z >> 2).is(BiomeTags.IS_BEACH)) {
                            beach++;
                            if (chunk.getBlockState(pos.set(x, y, z)).is(Blocks.SAND)) sand++;
                        }
                    }
                row.addProperty("beachColumns", beach); row.addProperty("surfaceSandColumns", sand);
            }
            row.add("firstHoles", holes); sites.add(row);
        }
        observation.add("sites", sites); report.getAsJsonArray("observations").add(observation);
    }

    private static void finish(boolean complete) throws Exception {
        report.addProperty("complete", complete); report.addProperty("observedTicks", elapsed);
        report.addProperty("elapsedSeconds", (System.nanoTime() - began) / 1e9);
        Files.writeString(destination, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }
}

package design.inherent.skyweavecompat.probe;

import org.sathrek.sky_archipelago.worldgen.generator.field.model.IslandDescriptor;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.IslandPreview;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import design.inherent.skyweavecompat.terrain.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.config.SkyIslandGeneratorSettings;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.IslandFamily;
import java.nio.file.*;
import java.util.*;

/** Bounded native density survey. No chunk generation or neighboring-chunk loads. */
final class TerrainProbe {
    private TerrainProbe() {}

    static void checkEmbeddedSettings(SkyIslandChunkGenerator generator) {
        var actual = SkyIslandGeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE,
                SkyIslandGeneratorSettings.fromRuntimeSettings(generator.resolvedSkyIslandSettings())).getOrThrow();
        var expected = SkyIslandGeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE,
                SkyIslandGeneratorSettings.fromRuntimeSettings(GenerationConfiguration.resolve(
                        generator.embeddedSettings().orElseThrow().toRuntimeSettings()))).getOrThrow();
        if (!actual.equals(expected)) throw new AssertionError("Dedicated server ignored embedded preset settings");
    }

    static void run(MinecraftServer server, SkyIslandSettings baseline, Path output) throws Exception {
        var report = new JsonObject();
        report.addProperty("evidence", "native-density-samples; actual preset biome filtering; before carving/features");
        report.add("profile", new Gson().toJsonTree(GenerationConfiguration.profile()));
        var rows = new JsonArray(); long started = System.nanoTime();
        var requestedSeed = Long.getLong("neighbors.probe.terrainSeed");
        long[] seeds = requestedSeed == null ? new long[]{1, 42, 8675309, 424242, -3095298318114116683L} : new long[]{requestedSeed};
        for (long seed : seeds) {
            for (boolean adapted : new boolean[]{false, true}) {
                var preset = server.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                        .get(ResourceLocation.parse(adapted ? GenerationProfile.ID : "sky_archipelago:sky_islands"));
                var generator = (SkyIslandChunkGenerator)preset.createWorldDimensions().overworld();
                var selectedSettings = generator.resolvedSkyIslandSettings();
                if (adapted) checkEmbeddedSettings(generator);
                var random = RandomState.create(generator.generatorSettings().value(),
                        server.registryAccess().registryOrThrow(Registries.NOISE).asLookup(), seed);
                var field = ((GeologicalGenerator)(Object)generator).skyweave$field(random);
                var previews = field.collectIslandPreviewsInRadius(0, 0, 16000, selectedSettings);
                var anchors = previews.stream().filter(p -> p.family() == IslandFamily.ANCHOR_PLATEAU)
                        .sorted(Comparator.comparingDouble(p -> Math.hypot(p.x(), p.z()))).toList();
                var row = new JsonObject(); row.addProperty("seed", seed); row.addProperty("adapted", adapted);
                row.add("settings", SkyIslandGeneratorSettings.CODEC.encodeStart(JsonOps.INSTANCE,
                        SkyIslandGeneratorSettings.fromRuntimeSettings(selectedSettings)).getOrThrow());
                row.add("descriptors", new Gson().toJsonTree(previews));
                var distances = new JsonArray();
                for (var anchor : anchors) {
                    if (Math.hypot(anchor.x(), anchor.z()) > 10000) continue;
                    double nearest = anchors.stream().filter(p -> p != anchor)
                            .mapToDouble(p -> Math.hypot(p.x() - anchor.x(), p.z() - anchor.z())).min().orElseThrow();
                    distances.add(nearest);
                }
                row.add("nearestAnchorDistances", distances);
                if (adapted && seed == 42) row.add("queryChecks", LayoutProbe.run(field, selectedSettings));
                if (adapted) {
                    var profile = ((ProfiledTerrain)(Object)field).skyweave$profile();
                    var clearances = new JsonArray();
                    var ownership = new HashMap<IslandPreview, ClusterLayout.Cell>();
                    for (var preview : previews) ownership.put(preview, owner(field, selectedSettings, profile, preview));
                    var groups = new JsonArray();
                    for (var cell : ownership.values().stream().distinct().sorted(Comparator.comparingInt(ClusterLayout.Cell::x)
                            .thenComparingInt(ClusterLayout.Cell::z)).toList()) {
                        var group = new JsonObject(); group.add("cell", new Gson().toJsonTree(cell));
                        group.add("members", new Gson().toJsonTree(((ClusterField)(Object)field).skyweave$members(cell.x(), cell.z(), selectedSettings)));
                        groups.add(group);
                    }
                    row.add("clusters", groups);
                    for (var anchor : anchors) {
                        if (Math.hypot(anchor.x(), anchor.z()) > 10000) continue;
                        var members = previews.stream().filter(p -> ownership.get(anchor).equals(ownership.get(p))).toList();
                        double gap = Double.POSITIVE_INFINITY;
                        for (var member : members) for (var other : previews) {
                            if (ownership.get(anchor).equals(ownership.get(other))) continue;
                            var a = member.terrainPlan().bounds();
                            var b = other.terrainPlan().bounds();
                            long dx = Math.max(0L, Math.max((long)a.minX() - b.maxX(), (long)b.minX() - a.maxX()));
                            long dz = Math.max(0L, Math.max((long)a.minZ() - b.maxZ(), (long)b.minZ() - a.maxZ()));
                            gap = Math.min(gap, Math.hypot(dx, dz));
                        }
                        if (gap < profile.minimumClusterGap()) throw new AssertionError("Native cluster envelope gap " + gap);
                        var clearance = new JsonObject();
                        clearance.addProperty("x", anchor.x()); clearance.addProperty("z", anchor.z());
                        clearance.addProperty("centerY", anchor.y()); clearance.addProperty("members", members.size());
                        clearance.addProperty("minimumSupportGap", gap); clearances.add(clearance);
                    }
                    row.add("clusterClearances", clearances);
                    var footprintRows = new JsonArray();
                    var footprints = new HashMap<IslandPreview, List<int[]>>();
                    for (var anchor : anchors.stream().limit(3).toList()) {
                        var nearest = anchors.stream().filter(p -> p != anchor)
                                .min(Comparator.comparingDouble(p -> Math.hypot(p.x() - anchor.x(), p.z() - anchor.z()))).orElseThrow();
                        var a = footprints.computeIfAbsent(anchor, key -> footprint(field, selectedSettings, profile, key));
                        var b = footprints.computeIfAbsent(nearest, key -> footprint(field, selectedSettings, profile, key));
                        double distance = Double.POSITIVE_INFINITY;
                        for (var ap : a) for (var bp : b) distance = Math.min(distance,
                                Math.hypot(ap[0] - bp[0], ap[1] - bp[1]));
                        var sample = new JsonObject();
                        sample.addProperty("x", anchor.x()); sample.addProperty("z", anchor.z());
                        sample.addProperty("neighborX", nearest.x()); sample.addProperty("neighborZ", nearest.z());
                        sample.addProperty("sampledShoreGap", distance);
                        sample.addProperty("gridStep", 16);
                        sample.addProperty("evidence", "Native solid-column footprints including companions; nearest-center pair; sampled distance, not a conservative bound or navigated route");
                        footprintRows.add(sample);
                    }
                    row.add("clusterFootprints", footprintRows);
                }
                var islands = new JsonArray();
                for (var anchor : anchors.stream().limit(Integer.getInteger("neighbors.probe.samples", 4)).toList()) {
                    int step = adapted ? 16 : Math.max(8, anchor.radius() / 4);
                    int extent = adapted ? (int)Math.ceil((double)GenerationProfile.supportRadius(anchor.radius()) / step) : 5;
                    var island = new JsonObject(); island.add("descriptor", new Gson().toJsonTree(anchor));
                    var columns = new JsonArray(); long estimatedVolume = 0;
                    int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE, occupied = 0;
                    for (int dx = -extent; dx <= extent; dx++) for (int dz = -extent; dz <= extent; dz++) {
                        int x = anchor.x() + dx * step, z = anchor.z() + dz * step;
                        var descriptor = field.sampleDominantIslandDescriptor(x, z, selectedSettings);
                        if (descriptor == null || descriptor.family() != IslandFamily.ANCHOR_PLATEAU
                                || descriptor.centerX() != anchor.x() || descriptor.centerZ() != anchor.z()) continue;
                        var segments = field.sampleSolidSegments(x, z, selectedSettings);
                        int thickness = segments.stream().mapToInt(s -> s.thickness()).sum();
                        if (thickness == 0) continue;
                        occupied++;
                        for (var segment : segments) {
                            minY = Math.min(minY, segment.bottomY()); maxY = Math.max(maxY, segment.topY());
                            if (adapted) {
                                var p = ((ProfiledTerrain)(Object)field).skyweave$profile();
                                if (segment.bottomY() < GenerationProfile.MIN_Y + p.floorClearance()
                                        || segment.topY() > GenerationProfile.MAX_Y - 1 - p.buildingHeadroom()) {
                                    throw new AssertionError("Terrain violates placement envelope at " + x + "," + z);
                                }
                            }
                        }
                        var column = new JsonObject(); column.addProperty("x", x); column.addProperty("z", z);
                        column.addProperty("solid", thickness); column.addProperty("interior", (long)dx * dx * step * step + (long)dz * dz * step * step <= (long)anchor.radius() * anchor.radius() / 4);
                        column.add("segments", new Gson().toJsonTree(segments)); columns.add(column);
                        estimatedVolume += (long)thickness * step * step;
                    }
                    if (adapted) {
                        var members = ((ClusterField)(Object)field).skyweave$members(
                                Math.floorDiv(anchor.x(), GenerationConfiguration.profile().spacing()),
                                Math.floorDiv(anchor.z(), GenerationConfiguration.profile().spacing()), selectedSettings);
                        var anchorShape = members.getFirst();
                        island.add("shape", new Gson().toJsonTree(anchorShape));
                        var frame = IslandGeometry.geologicalFrame(anchorShape);
                        island.addProperty("nominalBodyHeight", frame.topExclusive() - frame.bottom());
                    }
                    island.addProperty("step", step); island.addProperty("occupiedSamples", occupied);
                    island.addProperty("sampledMinY", occupied == 0 ? 0 : minY);
                    island.addProperty("sampledMaxY", occupied == 0 ? 0 : maxY);
                    island.addProperty("estimatedSolidVolume", estimatedVolume); island.add("columns", columns);
                    islands.add(island);
                    System.out.println("SKYWEAVE_TERRAIN " + seed + " adapted=" + adapted + " anchor="
                            + anchor.x() + "," + anchor.z() + " samples=" + occupied + " y=" + minY + ".." + maxY);
                }
                row.add("islands", islands); rows.add(row);
            }
        }
        report.addProperty("elapsedSeconds", (System.nanoTime() - started) / 1e9);
        report.add("runs", rows);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }

    private static List<int[]> footprint(IslandField field, SkyIslandSettings settings,
            GenerationProfile profile, IslandPreview anchor) {
        var target = owner(field,settings,profile,anchor);
        int step = 16, reach = profile.clusterReach();
        int minX = Math.floorDiv(anchor.x() - reach, step) * step;
        int minZ = Math.floorDiv(anchor.z() - reach, step) * step;
        int size = (2 * reach) / step + 3;
        var occupied = new boolean[size][size];
        for (int ix = 0; ix < size; ix++) for (int iz = 0; iz < size; iz++) {
            int x = minX + ix * step, z = minZ + iz * step;
            var descriptor = field.sampleDominantIslandDescriptor(x,z,settings);
            if (descriptor == null || !((ClusterField)(Object)field).skyweave$owner(descriptor,settings).equals(target)) continue;
            var segments = field.sampleSolidSegments(x, z, settings);
            occupied[ix][iz] = !segments.isEmpty();
            for (var segment : segments) {
                if (segment.bottomY() < GenerationProfile.MIN_Y + profile.floorClearance()
                        || segment.topY() > GenerationProfile.MAX_Y - 1 - profile.buildingHeadroom()) {
                    throw new AssertionError("Full cluster sample exceeds vertical envelope at " + x + "," + z);
                }
            }
        }
        var edge = new ArrayList<int[]>();
        for (int ix = 0; ix < size; ix++) for (int iz = 0; iz < size; iz++) {
            if (occupied[ix][iz] && (ix == 0 || iz == 0 || ix == size - 1 || iz == size - 1
                    || !occupied[ix - 1][iz] || !occupied[ix + 1][iz]
                    || !occupied[ix][iz - 1] || !occupied[ix][iz + 1])) {
                edge.add(new int[]{minX + ix * step, minZ + iz * step});
            }
        }
        if (edge.isEmpty()) throw new AssertionError("Empty native footprint");
        return edge;
    }

    private static ClusterLayout.Cell owner(IslandField field, SkyIslandSettings settings,
            GenerationProfile profile, IslandPreview preview) {
        var membership = (ClusterField)(Object)field;
        int cx = Math.floorDiv(preview.x(), profile.spacing()), cz = Math.floorDiv(preview.z(), profile.spacing());
        int range = profile.lookupCells();
        for (int dx = -range; dx <= range; dx++) for (int dz = -range; dz <= range; dz++) {
            for (var member : membership.skyweave$members(cx + dx, cz + dz, settings)) {
                if (member.centerX() == preview.x() && member.centerY() == preview.y() && member.centerZ() == preview.z()
                        && member.family() == preview.family()) return new ClusterLayout.Cell(cx + dx, cz + dz);
            }
        }
        throw new AssertionError("Preview has no accepted cluster membership");
    }
}

package design.inherent.skyvillageneighbors.probe;

import com.google.gson.*;
import design.inherent.skyvillageneighbors.NeighborConfig;
import design.inherent.skyvillageneighbors.placement.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.structure.*;
import java.nio.file.*;
import java.util.*;

@Mod("sky_village_neighbors_probe")
public final class PlacementProbe {
    private static final ResourceLocation VILLAGE = ResourceLocation.fromNamespaceAndPath("skyvillages", "skyvillage");
    public PlacementProbe() { NeoForge.EVENT_BUS.addListener(this::run); }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    private static void setPolicy(NeighborRule.Settings settings) throws Exception {
        var field = NeighborConfig.class.getDeclaredField("active");
        field.setAccessible(true);
        field.set(null, settings);
    }
    private void natural(net.minecraft.server.MinecraftServer server,
            net.minecraft.world.level.levelgen.structure.Structure structure) throws Exception {
        var level = server.overworld();
        var generator = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var settings = generator.resolvedSkyIslandSettings();
        var getter = generator.getClass().getDeclaredMethod("getIslandField", RandomState.class);
        getter.setAccessible(true);
        var field = (IslandField)getter.invoke(generator, level.getChunkSource().randomState());
        var terrain = new IslandTerrain(new StructureSupportContext(VILLAGE, settings, field));
        var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)
                server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(VILLAGE).placement();
        JsonArray rows = new JsonArray(); int accepted = 0, completed = 0;
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            var candidate = placement.getPotentialStructureChunk(level.getSeed(), x * placement.spacing(), z * placement.spacing());
            var chunk = level.getChunk(candidate.x, candidate.z, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            var start = chunk.getStartForStructure(structure);
            JsonObject row = new JsonObject(); row.addProperty("x", candidate.x); row.addProperty("z", candidate.z);
            boolean valid = start != null && start.isValid(); row.addProperty("accepted", valid);
            if (valid) {
                accepted++;
                var b = start.getBoundingBox();
                check(NeighborRule.evaluate(new NeighborRule.Footprint(b.minX(), b.minZ(), b.maxX(), b.maxZ()),
                        NeighborConfig.settings(), terrain) == NeighborRule.Decision.ACCEPTED, "Natural start violates placement rule");
                check(b.minY() >= level.getMinBuildHeight() && b.maxY() < level.getMaxBuildHeight(), "Vertical bounds");
                if (completed < 2) {
                    for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++)
                        for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) level.getChunk(cx, cz);
                    int nonAir = 0;
                    var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
                    for (int bx = b.minX(); bx <= b.maxX(); bx++) for (int bz = b.minZ(); bz <= b.maxZ(); bz++)
                        for (int by = b.minY(); by <= b.maxY(); by++) {
                            if (!level.getBlockState(pos.set(bx, by, bz)).isAir()) nonAir++;
                        }
                    check(nonAir > 100, "Registered layout has no realized structure");
                    row.addProperty("blocks", nonAir); row.addProperty("pieces", start.getPieces().size());
                    completed++;
                }
            }
            rows.add(row);
        }
        check(accepted > 0 && completed > 0, "No full natural villages exercised");
        JsonObject report = new JsonObject(); report.addProperty("seed", level.getSeed());
        report.add("settings", new Gson().toJsonTree(NeighborConfig.settings()));
        report.addProperty("accepted", accepted); report.addProperty("completed", completed); report.add("samples", rows);
        Files.writeString(Path.of(System.getProperty("neighbors.probe.output")), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        System.out.println("NEIGHBORS_NATURAL_COMPLETE " + accepted + " starts; " + completed + " complete villages");
    }

    private void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            check(NeighborConfig.settings().maximumGap() == Integer.getInteger("neighbors.probe.expectedMaximumGap", 512), "Config not applied at server start");
            var preset = server.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                    .get(ResourceLocation.fromNamespaceAndPath("sky_archipelago", "sky_islands"));
            var generator = (SkyIslandChunkGenerator)preset.createWorldDimensions().overworld();
            var settings = generator.resolvedSkyIslandSettings();
            var getter = generator.getClass().getDeclaredMethod("getIslandField", RandomState.class);
            getter.setAccessible(true);
            var validator = new StructureSupportValidator((a, b, c) -> { throw new AssertionError("Grounding called"); });
            var structure = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(VILLAGE);
            var level = server.overworld();
            var flat = level.getChunkSource().getGenerator();
            if (Boolean.getBoolean("neighbors.probe.natural")) {
                natural(server, structure);
                return;
            }
            var policies = List.of(new NeighborRule.Settings(true, 64, 384, 96),
                    new NeighborRule.Settings(true, 64, 512, 96), new NeighborRule.Settings(true, 64, 768, 96));
            JsonArray results = new JsonArray();
            int mixinRejections = 0, nativeAccepted = 0;
            for (long seed : new long[]{1, 42, 8675309, 424242}) {
                var random = RandomState.create(server.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS)
                        .getOrThrow(NoiseGeneratorSettings.OVERWORLD), server.registryAccess().registryOrThrow(Registries.NOISE).asLookup(), seed);
                var field = (IslandField)getter.invoke(generator, random);
                var context = new StructureSupportContext(VILLAGE, settings, field);
                var terrain = new IslandTerrain(context);
                // Layout-only samples isolate the predicate from biome rejection and chunk visitation.
                int samples = Integer.getInteger("neighbors.probe.samples", 12);
                for (int index = 0; index < samples; index++) {
                    int x = (index % 4 - 2) * 70 + 2, z = (index / 4 - 1) * 70 + 2;
                    StructureStart start = structure.generate(server.registryAccess(), flat, flat.getBiomeSource(),
                            level.getChunkSource().randomState(), server.getStructureManager(), seed, new ChunkPos(x, z), 0, level, biome -> true);
                    check(start.isValid(), "Native layout invalid");
                    setPolicy(new NeighborRule.Settings(false, 64, 512, 96));
                    var baseline = validator.evaluatePlacement(context, start, StructurePlacementCategory.SKY);
                    if (!baseline.accepted()) continue;
                    nativeAccepted++;
                    var b = start.getBoundingBox();
                    var footprint = new NeighborRule.Footprint(b.minX(), b.minZ(), b.maxX(), b.maxZ());
                    JsonObject row = new JsonObject(); row.addProperty("seed", seed); row.addProperty("index", index);
                    row.addProperty("width", b.getXSpan()); row.addProperty("depth", b.getZSpan());
                    JsonArray variants = new JsonArray();
                    for (var policy : policies) {
                        long began = System.nanoTime();
                        var decision = NeighborRule.evaluate(footprint, policy, terrain);
                        JsonObject variant = new JsonObject(); variant.addProperty("maximumGap", policy.maximumGap());
                        variant.addProperty("decision", decision.name()); variant.addProperty("ms", (System.nanoTime() - began) / 1_000_000.0);
                        variants.add(variant);
                        if (policy.maximumGap() == 512) {
                            setPolicy(policy);
                            var patched = validator.evaluatePlacement(context, start, StructurePlacementCategory.SKY);
                            check(patched.accepted() == (decision == NeighborRule.Decision.ACCEPTED), "Mixin differs from native predicate");
                            if (!patched.accepted()) mixinRejections++;
                            check(start.getBoundingBox().equals(b), "Layout moved");
                            var otherContext = new StructureSupportContext(ResourceLocation.withDefaultNamespace("shipwreck"), settings, field);
                            var other = validator.evaluatePlacement(otherContext, start, StructurePlacementCategory.SKY);
                            check(other.accepted(), "Unrelated SKY structure filtered");
                        }
                    }
                    row.add("variants", variants); results.add(row);
                    System.out.println("NEIGHBORS_SAMPLE " + row);
                }
            }
            check(nativeAccepted > 0 && mixinRejections > 0, "Did not exercise Mixin rejection");
            JsonObject report = new JsonObject(); report.addProperty("nativeAccepted", nativeAccepted);
            report.addProperty("mixinRejections", mixinRejections); report.add("samples", results);
            Files.writeString(Path.of(System.getProperty("neighbors.probe.output")), new GsonBuilder().setPrettyPrinting().create().toJson(report));
            System.out.println("NEIGHBORS_PROBE_COMPLETE");
        } catch (Throwable exception) { exception.printStackTrace(); }
        finally { server.halt(false); }
    }
}

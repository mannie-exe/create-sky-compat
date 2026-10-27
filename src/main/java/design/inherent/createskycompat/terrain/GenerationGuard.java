package design.inherent.createskycompat.terrain;

import com.google.gson.*;
import design.inherent.createskycompat.NeighborConfig;
import design.inherent.createskycompat.OptionalIntegrations;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.storage.LevelResource;
import org.sathrek.sky_archipelago.config.SkyIslandGeneratorSettings;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.util.TreeSet;

public final class GenerationGuard {
    private GenerationGuard() {}
    public static void verify(MinecraftServer server) {
        var root = server.getWorldPath(LevelResource.ROOT);
        var stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).getOrThrow(LevelStem.OVERWORLD);
        if (!(stem.generator() instanceof SkyIslandChunkGenerator sky)
                || !((GeologicalGenerator)(Object)sky).createSky$isEnabled()) {
            if (Files.exists(root.resolve(GenerationIdentity.FILE))) {
                throw new IllegalStateException("This world requires its recorded Create Sky generator; refusing an incompatible replacement.");
            }
            return;
        }
        if (stem.type().value().minY() != GenerationProfile.MIN_Y
                || stem.type().value().height() != GenerationProfile.MAX_Y - GenerationProfile.MIN_Y
                || sky.getMinY() != GenerationProfile.MIN_Y
                || sky.getGenDepth() != GenerationProfile.MAX_Y - GenerationProfile.MIN_Y) {
            throw new IllegalStateException("Create Sky dimension and noise settings must both use [-128, 768)");
        }
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        var identity = new JsonObject();
        identity.addProperty("algorithm", "islands_v1-prototype-10");
        identity.addProperty("largeVeins", "native_reference_depth_v1");
        identity.addProperty("seed", server.getWorldData().worldGenOptions().seed());
        identity.add("villagePolicy", new Gson().toJsonTree(NeighborConfig.settings()));
        var villages = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.parse("skyvillages:skyvillage"));
        if (OptionalIntegrations.skyVillages() && villages != null) {
            identity.add("villageSource", StructureSet.DIRECT_CODEC.encodeStart(ops, villages).getOrThrow());
        }
        identity.add("profile", new Gson().toJsonTree(GenerationConfiguration.profile()));
        identity.add("interiors", new Gson().toJsonTree(InteriorConfiguration.profile()));
        identity.add("water", new Gson().toJsonTree(WaterConfiguration.profile()));
        identity.addProperty("waterPolicy", "owned_sources_and_lining_v1");
        identity.add("oceans", new Gson().toJsonTree(OceanConfiguration.profile()));
        var mudBiomes = new JsonArray();
        server.registryAccess().registryOrThrow(Registries.BIOME).getTag(BiomeMaterials.MUD).ifPresent(tag ->
                tag.stream().map(biome -> biome.getRegisteredName()).sorted().forEach(mudBiomes::add));
        identity.add("mudSurfaceBiomes", mudBiomes);
        identity.addProperty("geologicalSurfaceReference", GeologicalFrame.REFERENCE_SURFACE);
        identity.addProperty("canyonOrigins", "local_surface");
        identity.addProperty("ancientCityEligibility", "lower_quarter_v1");
        identity.addProperty("spawnLanding", "generated_surface");
        identity.addProperty("climate", "biome_temperature_at_reference_surface");
        identity.add("biomes", net.minecraft.world.level.biome.BiomeSource.CODEC.encodeStart(ops, sky.getBiomeSource()).getOrThrow());
        identity.add("islands", SkyIslandGeneratorSettings.CODEC.encodeStart(ops,
                SkyIslandGeneratorSettings.fromRuntimeSettings(sky.resolvedSkyIslandSettings())).getOrThrow());
        identity.add("noise", NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops, sky.generatorSettings().value()).getOrThrow());
        identity.add("dimension", DimensionType.DIRECT_CODEC.encodeStart(ops, stem.type().value()).getOrThrow());
        try {
            GenerationIdentity.verify(root, new GsonBuilder().setPrettyPrinting().create().toJson(canonical(identity)) + "\n");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not verify Create Sky world generation identity", exception);
        }
    }

    private static JsonElement canonical(JsonElement value) {
        if (value.isJsonObject()) {
            var sorted = new JsonObject();
            for (String key : new TreeSet<>(value.getAsJsonObject().keySet())) sorted.add(key, canonical(value.getAsJsonObject().get(key)));
            return sorted;
        }
        if (value.isJsonArray()) {
            var array = new JsonArray(); for (var entry : value.getAsJsonArray()) array.add(canonical(entry)); return array;
        }
        return value;
    }
}

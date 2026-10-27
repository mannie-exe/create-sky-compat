package design.inherent.createskycompat.terrain;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

/** Stored habitat cells near real local water; no global climate or sea-level replacement. */
public final class OceanBiomes {
    private OceanBiomes() {}
    public static Holder<Biome> project(BiomeSource source, Holder<Biome> original, IslandField field,
            SkyIslandSettings settings, int x, int y, int z) {
        if (!OceanConfiguration.profile().enabled()) return original;
        var water = ((InteriorField)(Object)field).createSky$waterAt(x, z, settings);
        for (var region : water.regions()) {
            if (!region.ocean() || y < region.bed(x, z) - 4 || y > region.level() + 8) continue;
            boolean wet = region.basin(x, z) && !water.sources().isEmpty();
            boolean beach = region.kind() == WaterRegion.Kind.OVERFLOW && !region.basin(x, z)
                    && region.affects(x, z) && field.sampleColumnProfile(x, z, settings).topY() <= region.level() + 6;
            if (!wet && !beach) continue;
            float temperature = original.value().getBaseTemperature();
            var key = beach ? (temperature <= 0 ? Biomes.SNOWY_BEACH : Biomes.BEACH)
                    : temperature <= 0 ? Biomes.FROZEN_OCEAN : temperature < 0.45 ? Biomes.COLD_OCEAN
                    : temperature >= 1 ? Biomes.WARM_OCEAN : Biomes.OCEAN;
            return source.possibleBiomes().stream().filter(biome -> biome.is(key)).findFirst().orElse(original);
        }
        return original;
    }
}

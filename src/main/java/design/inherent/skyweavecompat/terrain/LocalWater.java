package design.inherent.skyweavecompat.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;

/** Local context for predicates that otherwise assume a single global sea level. */
public final class LocalWater {
    private LocalWater() {}

    public static int spawnSurface(LevelAccessor access, BlockPos position, int fallback) {
        if (!(access instanceof ServerLevelAccessor server)) return fallback;
        var level = server.getLevel();
        if (!(level.getChunkSource().getGenerator() instanceof SkyIslandChunkGenerator generator)) return fallback;
        var geology = (GeologicalGenerator)(Object)generator;
        if (!geology.skyweave$isEnabled()) return fallback;
        var field = geology.skyweave$field(level.getChunkSource().randomState());
        var settings = generator.resolvedSkyIslandSettings();
        var water = field.sampleWaterArchetypeColumnProfile(position.getX(), position.getZ(), settings);
        if (water.containsWaterAt(position.getY())) return water.fluidSurfaceY();
        var river = field.sampleHydrologyColumn(position.getX(), position.getZ(), settings);
        return river.containsFluidAt(position.getY()) ? river.fluidSurfaceY() : fallback;
    }
}

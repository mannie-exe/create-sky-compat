package design.inherent.createskycompat.terrain;

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
        if (!geology.createSky$isEnabled()) return fallback;
        var field = (InteriorField)(Object)geology.createSky$field(level.getChunkSource().randomState());
        var water = field.createSky$waterAt(position.getX(), position.getZ(), generator.resolvedSkyIslandSettings());
        if (!water.contains(position.getY())) return fallback;
        for (var region : water.regions()) {
            if (region.basin(position.getX(), position.getZ()) && position.getY() > region.bed(position.getX(), position.getZ())
                    && position.getY() <= region.level()) return region.level();
        }
        return fallback;
    }
}

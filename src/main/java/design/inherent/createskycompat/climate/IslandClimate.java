package design.inherent.createskycompat.climate;

import design.inherent.createskycompat.terrain.GenerationProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;

/** Temperature altitude only: block placement, light, fluid and collision coordinates stay unchanged. */
public final class IslandClimate {
    private static final ResourceLocation DIMENSION_TYPE = ResourceLocation.parse(GenerationProfile.ID);
    private IslandClimate() {}

    public static boolean applies(LevelReader reader) {
        Level level = reader instanceof Level value ? value : reader instanceof WorldGenLevel world ? world.getLevel() : null;
        return level != null && level.dimensionTypeRegistration().is(DIMENSION_TYPE);
    }

    public static BlockPos temperaturePosition(LevelReader level, BlockPos position) {
        return applies(level) ? temperaturePosition(position) : position;
    }

    public static BlockPos temperaturePosition(BlockPos position) {
        // Biome climate is independent of a cluster's altitude. This also keeps client weather and
        // server freezing identical without reconstructing hidden server terrain on the client.
        // Cold-biome base temperatures and X/Z temperature noise remain native.
        return position.atY(64);
    }
}

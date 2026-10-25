package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import design.inherent.createskycompat.climate.IslandClimate;
import design.inherent.createskycompat.climate.SurfaceClimateScope;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.surface.SkyIslandSurfacePass;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SkyIslandSurfacePass.class, remap = false)
public abstract class SurfaceClimateMixin {
    @WrapMethod(method = "buildSurface")
    private static void surface(WorldGenRegion level, StructureManager structures, ChunkAccess chunk,
            SkyIslandSettings settings, IslandField field, Operation<Void> original) {
        SurfaceClimateScope.run(IslandClimate.applies(level), () -> original.call(level, structures, chunk, settings, field));
    }
}

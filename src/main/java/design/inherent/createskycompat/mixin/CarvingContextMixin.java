package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvingContext.class)
public abstract class CarvingContextMixin implements GeologicalCarving {
    @Unique private CarverGeology createSky$geology;
    @Override public CarverGeology createSky$geology() { return createSky$geology; }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bind(NoiseBasedChunkGenerator generator, RegistryAccess registry, LevelHeightAccessor level,
            NoiseChunk noise, RandomState random, SurfaceRules.RuleSource surface, CallbackInfo callback) {
        if (generator instanceof SkyIslandChunkGenerator sky && ((GeologicalGenerator)(Object)sky).createSky$isEnabled()) {
            createSky$geology = new CarverGeology(sky, ((GeologicalGenerator)(Object)sky).createSky$field(random));
        }
    }
}

package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.terrain.LocalWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WaterAnimal.class)
public abstract class WaterAnimalMixin {
    @WrapOperation(method = "checkSurfaceWaterAnimalSpawnRules", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;getSeaLevel()I"))
    private static int localSurface(LevelAccessor level, Operation<Integer> original, @Local(argsOnly = true) BlockPos position) {
        return LocalWater.spawnSurface(level, position, original.call(level));
    }
}

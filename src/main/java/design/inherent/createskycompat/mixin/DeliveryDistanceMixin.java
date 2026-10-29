package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.placement.DeliveryChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.ladderstar.adq.QuestGenerator", remap = false)
public abstract class DeliveryDistanceMixin {
    @WrapOperation(method = "lambda$generateNewQuestAsync$9", require = 2, allow = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private static double createSky$horizontalRouteDistance(BlockPos point, Vec3i other, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        if (!DeliveryChunks.applies(level)) return original.call(point, other);
        // Locator Y can be zero before realization and ~500 afterward, even at the same village.
        double dx = (double)point.getX() - other.getX(), dz = (double)point.getZ() - other.getZ();
        return dx * dx + dz * dz;
    }

}

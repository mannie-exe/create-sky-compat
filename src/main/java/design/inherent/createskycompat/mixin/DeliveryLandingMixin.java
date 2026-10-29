package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import design.inherent.createskycompat.placement.VillageDeliveryLanding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.ladderstar.adq.QuestGenerator", remap = false)
public abstract class DeliveryLandingMixin {
    @WrapOperation(method = "lambda$generateNewQuestAsync$4", require = 2, allow = 2,
            at = @At(value = "INVOKE", target = "Lcom/ladderstar/adq/QuestGenerator;resolveDrySurface(Lnet/minecraft/server/level/ServerLevel;II)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos createSky$deliveryLanding(ServerLevel level, int x, int z, Operation<BlockPos> original) {
        var result = original.call(level, x, z);
        return result != null ? result : VillageDeliveryLanding.find(level, x, z, (world, px, pz) -> original.call(world, px, pz));
    }
}

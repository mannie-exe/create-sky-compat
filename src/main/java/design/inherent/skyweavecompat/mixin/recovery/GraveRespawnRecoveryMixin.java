package design.inherent.skyweavecompat.mixin.recovery;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.events.ServerEventHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import design.inherent.skyweavecompat.recovery.ShipGraves;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerEventHandler.class, remap = false)
public abstract class GraveRespawnRecoveryMixin {
    @WrapOperation(method = "afterRespawn", at = @At(value = "INVOKE",
            target = "Lcom/b1n_ry/yigd/components/GraveComponent;getPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos skyweave$projectNotice(GraveComponent grave, Operation<BlockPos> original) {
        return ShipGraves.anchor(grave) == null ? original.call(grave) : ShipGraves.worldPosition(grave);
    }
}

package design.inherent.createskycompat.mixin.linear;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import design.inherent.createskycompat.linear.MagneticStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.bearing.linearbearing.blockentity.MagneticPortBlockEntity", remap = false)
public abstract class MagneticPortMixin extends GeneratingKineticBlockEntity implements MagneticStatus {
    @Shadow private boolean transmitting;
    @Shadow private BlockPos visualTargetPos;
    protected MagneticPortMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Inject(method = "calculateStressApplied", at = @At("HEAD"), cancellable = true)
    private void createSky$ordinaryNetworkLoad(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(super.calculateStressApplied());
    }
    @Override public void createSky$linked(BlockPos target) {
        boolean active = target != null && getSpeed() != 0;
        if (transmitting != active || !java.util.Objects.equals(visualTargetPos, target)) {
            transmitting = active; visualTargetPos = target; setChanged(); sendData();
        }
    }
}

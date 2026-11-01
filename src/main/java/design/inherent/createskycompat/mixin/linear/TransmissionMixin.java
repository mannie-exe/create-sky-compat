package design.inherent.createskycompat.mixin.linear;

import com.simibubi.create.content.kinetics.base.*;
import design.inherent.createskycompat.linear.TransmissionEndpoint;
import design.inherent.createskycompat.linear.TransmissionLinks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = {"com.bearing.linearbearing.blockentity.MagneticPortBlockEntity",
        "com.bearing.linearbearing.blockentity.TorsionalAnchorBlockEntity"}, remap = false)
public abstract class TransmissionMixin extends GeneratingKineticBlockEntity implements TransmissionEndpoint {
    @Unique private KineticBlockEntity createSky$peer;
    protected TransmissionMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public KineticBlockEntity createSky$peer() { return createSky$peer; }
    @Override public void createSky$peer(KineticBlockEntity peer) { createSky$peer = peer; }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void createSky$nativeTransmission(CallbackInfo callback) {
        super.tick();
        if (level != null && !level.isClientSide()) TransmissionLinks.tick(this);
        callback.cancel();
    }
    @Inject(method = "getGeneratedSpeed", at = @At("HEAD"), cancellable = true)
    private void createSky$noGeneratedSpeed(CallbackInfoReturnable<Float> callback) { callback.setReturnValue(0f); }
    @Inject(method = "calculateAddedStressCapacity", at = @At("HEAD"), cancellable = true)
    private void createSky$noGeneratedCapacity(CallbackInfoReturnable<Float> callback) { callback.setReturnValue(0f); }
    @Inject(method = "isSource", at = @At("HEAD"), cancellable = true)
    private void createSky$noGeneratedSource(CallbackInfoReturnable<Boolean> callback) { callback.setReturnValue(false); }

    @Override public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        var result = super.addPropagationLocations(block, state, neighbours);
        if (TransmissionLinks.connected(this, createSky$peer) && !result.contains(createSky$peer.getBlockPos()))
            result.add(createSky$peer.getBlockPos());
        return result;
    }
    @Override public float propagateRotationTo(KineticBlockEntity other, BlockState from, BlockState to,
            BlockPos difference, boolean shafts, boolean cogs) {
        return TransmissionLinks.connected(this, other) ? TransmissionLinks.ratio(this, other)
                : super.propagateRotationTo(other, from, to, difference, shafts, cogs);
    }
    @Override public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
        return TransmissionLinks.connected(this, other) || super.isCustomConnection(other, state, otherState);
    }
    @Override public void remove() {
        super.remove();
        if (level != null && !level.isClientSide()) TransmissionLinks.disconnect(this);
    }
}

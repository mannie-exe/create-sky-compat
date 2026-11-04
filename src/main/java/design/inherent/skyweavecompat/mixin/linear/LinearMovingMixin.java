package design.inherent.skyweavecompat.mixin.linear;

import design.inherent.skyweavecompat.linear.LinearBrake;
import design.inherent.skyweavecompat.linear.BrakeRail;
import design.inherent.skyweavecompat.linear.BrakedCarriage;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.bearing.linearbearing.blockentity.LinearMovingBlockEntity", remap = false)
public abstract class LinearMovingMixin extends DockingConnectorBlockEntity implements BrakedCarriage {
    @Shadow private boolean isLocked;
    @Unique private LinearBrake skyweave$brake;
    protected LinearMovingMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Inject(method = "tick", at = @At("HEAD"))
    private void skyweave$validateBrake(CallbackInfo callback) {
        if (level != null && !level.isClientSide() && isLocked
                && (skyweave$brake == null || !skyweave$brake.valid(this))) skyweave$releaseBrake();
    }
    @Inject(method = "tryLockCarriageToCasing", at = @At("HEAD"), cancellable = true)
    private void skyweave$relativeBrake(CallbackInfo callback) {
        skyweave$brake = LinearBrake.attach(this);
        isLocked = skyweave$brake != null;
        if (isLocked) { state = DockingConnectorState.LOCKED; notifyUpdate(); }
        callback.cancel();
    }
    @Inject(method = "tryUnlockCarriageFromCasing", at = @At("HEAD"), cancellable = true)
    private void skyweave$unbrake(CallbackInfo callback) { skyweave$releaseBrake(); callback.cancel(); }

    @Override public void skyweave$releaseBrake() {
        var previous = skyweave$brake;
        skyweave$brake = null; isLocked = false;
        if (previous != null) ((BrakeRail) previous.casing()).skyweave$removeCarriage(this);
        unDock();
    }
    @Override public LinearBrake skyweave$brake() { return skyweave$brake; }
    @Override public Iterable<SubLevel> sable$getConnectionDependencies() {
        return skyweave$brake == null || skyweave$brake.casingOwner() == null ? java.util.List.of()
                : java.util.List.of(skyweave$brake.casingOwner());
    }
    @Override public void invalidate() {
        if (skyweave$brake != null) skyweave$releaseBrake();
        super.invalidate();
    }
    @Override public void remove() {
        if (skyweave$brake != null) skyweave$releaseBrake();
        super.remove();
    }
}

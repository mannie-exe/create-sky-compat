package design.inherent.createskycompat.mixin.linear;

import design.inherent.createskycompat.linear.LinearBrake;
import design.inherent.createskycompat.linear.BrakeRail;
import design.inherent.createskycompat.linear.BrakedCarriage;
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
    @Unique private LinearBrake createSky$brake;
    protected LinearMovingMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Inject(method = "tick", at = @At("HEAD"))
    private void createSky$validateBrake(CallbackInfo callback) {
        if (level != null && !level.isClientSide() && isLocked
                && (createSky$brake == null || !createSky$brake.valid(this))) createSky$releaseBrake();
    }
    @Inject(method = "tryLockCarriageToCasing", at = @At("HEAD"), cancellable = true)
    private void createSky$relativeBrake(CallbackInfo callback) {
        createSky$brake = LinearBrake.attach(this);
        isLocked = createSky$brake != null;
        if (isLocked) { state = DockingConnectorState.LOCKED; notifyUpdate(); }
        callback.cancel();
    }
    @Inject(method = "tryUnlockCarriageFromCasing", at = @At("HEAD"), cancellable = true)
    private void createSky$unbrake(CallbackInfo callback) { createSky$releaseBrake(); callback.cancel(); }

    @Override public void createSky$releaseBrake() {
        var previous = createSky$brake;
        createSky$brake = null; isLocked = false;
        if (previous != null) ((BrakeRail) previous.casing()).createSky$removeCarriage(this);
        unDock();
    }
    @Override public LinearBrake createSky$brake() { return createSky$brake; }
    @Override public Iterable<SubLevel> sable$getConnectionDependencies() {
        return createSky$brake == null || createSky$brake.casingOwner() == null ? java.util.List.of()
                : java.util.List.of(createSky$brake.casingOwner());
    }
    @Override public void invalidate() {
        if (createSky$brake != null) createSky$releaseBrake();
        super.invalidate();
    }
    @Override public void remove() {
        if (createSky$brake != null) createSky$releaseBrake();
        super.remove();
    }
}

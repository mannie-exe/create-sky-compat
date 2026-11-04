package design.inherent.skyweavecompat.mixin.redstone;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.siliconcarbidecube.create_redstone_additions.blocks.Crossroad;
import design.inherent.skyweavecompat.redstone.SignalRead;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Crossroad.class, remap = false)
public abstract class CrossroadMixin extends DiodeBlock {
    protected CrossroadMixin(BlockBehaviour.Properties properties) { super(properties); }
    @Shadow protected abstract int getFlankInputSignal(Level level, BlockPos pos, BlockState state);
    @Shadow protected abstract int getInputSignal(Level level, BlockPos pos, BlockState state);
    @Unique private final ThreadLocal<SignalRead> skyweave$reading = new ThreadLocal<>();
    @Unique private int[] skyweave$inputs(Level level, BlockPos pos, BlockState state) {
        SignalRead previous = skyweave$reading.get();
        skyweave$reading.set(new SignalRead(level, pos.immutable()));
        try { return new int[]{getInputSignal(level, pos, state), getFlankInputSignal(level, pos, state)}; }
        finally { if (previous == null) skyweave$reading.remove(); else skyweave$reading.set(previous); }
    }
    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        SignalRead reading = skyweave$reading.get();
        if (reading != null && reading.level() == level && reading.pos().equals(pos)) return 0;
        return Math.max(0, super.getDirectSignal(state, level, pos, direction) - 1);
    }
    @Unique private BlockState skyweave$state(BlockState state, int[] values) {
        int model = (values[0] > 0 ? 2 : 0) | (values[1] > 0 ? 1 : 0);
        return state.setValue(Crossroad.POWER, values[0]).setValue(Crossroad.FLANK_POWER, values[1]).setValue(Crossroad.MODEL_TYPE, model);
    }
    @ModifyReturnValue(method = "getStateForPlacement", at = @At("RETURN"), require = 1, allow = 1)
    private BlockState skyweave$placement(BlockState state, BlockPlaceContext context) {
        return state == null ? null : skyweave$state(state, skyweave$inputs(context.getLevel(), context.getClickedPos(), state));
    }
    @Inject(method = "shouldTurnOn", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$placementTick(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        int[] values = skyweave$inputs(level, pos, state); cir.setReturnValue(values[0] > 0 || values[1] > 0);
    }
    @Unique private void skyweave$update(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || isLocked(level, pos, state)) return;
        BlockState updated = skyweave$state(state, skyweave$inputs(level, pos, state));
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            updateNeighborsInFront(level, pos, updated);
        }
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        skyweave$update(level, pos, state); ci.cancel();
    }
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$neighbor(BlockState state, Level level, BlockPos pos, Block from, BlockPos fromPos, boolean moving, CallbackInfo ci) {
        if (!state.canSurvive(level, pos)) super.neighborChanged(state, level, pos, from, fromPos, moving);
        else skyweave$update(level, pos, state);
        ci.cancel();
    }
}

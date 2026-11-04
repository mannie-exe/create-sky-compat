package design.inherent.skyweavecompat.mixin.redstone;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.siliconcarbidecube.create_redstone_additions.blocks.Conjunctor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Input queries are pure; a settled input must not perpetually schedule itself. */
@Mixin(value = Conjunctor.class, remap = false)
public abstract class ConjunctorMixin extends DiodeBlock {
    protected ConjunctorMixin(BlockBehaviour.Properties properties) { super(properties); }
    @Unique private int skyweave$inputs(Level level, BlockPos pos, BlockState state) {
        Direction left = state.getValue(FACING).getClockWise(), right = left.getOpposite();
        return (level.getSignal(pos.relative(right), right) > 0 ? 1 : 0)
                | (level.getSignal(pos.relative(left), left) > 0 ? 2 : 0);
    }
    @Inject(method = "getInputSignal", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$read(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(skyweave$inputs(level, pos, state));
    }
    @ModifyReturnValue(method = "getStateForPlacement", at = @At("RETURN"), require = 1, allow = 1)
    private BlockState skyweave$placement(BlockState state, BlockPlaceContext context) {
        return state == null ? null : state.setValue(Conjunctor.POWER_TYPE, skyweave$inputs(context.getLevel(), context.getClickedPos(), state));
    }
    @Inject(method = "shouldTurnOn", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$placementTick(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(skyweave$inputs(level, pos, state) > 0);
    }
    @Unique private void skyweave$update(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide && !isLocked(level, pos, state)) {
            int next = skyweave$inputs(level, pos, state);
            if (state.getValue(Conjunctor.POWER_TYPE) != next) {
                BlockState updated = state.setValue(Conjunctor.POWER_TYPE, next);
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
                updateNeighborsInFront(level, pos, updated);
            }
        }
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$settledTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        skyweave$update(level, pos, state); ci.cancel();
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block from, BlockPos fromPos, boolean moving) {
        if (!state.canSurvive(level, pos)) super.neighborChanged(state, level, pos, from, fromPos, moving);
        else skyweave$update(level, pos, state);
    }
}

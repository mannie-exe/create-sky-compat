package design.inherent.createskycompat.mixin.redstone;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.siliconcarbidecube.create_redstone_additions.blocks.AbstractCustomDiodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = AbstractCustomDiodeBlock.class, remap = false)
public abstract class GateWrenchMixin extends DiodeBlock implements IWrenchable {
    protected GateWrenchMixin(BlockBehaviour.Properties properties) { super(properties); }
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().mayBuild()) return InteractionResult.FAIL;
        return IWrenchable.super.onWrenched(state, context);
    }
    @Override public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().mayBuild()) return InteractionResult.FAIL;
        return IWrenchable.super.onSneakWrenched(state, context);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        if (previous.is((net.minecraft.world.level.block.Block)(Object)this) && previous.getValue(FACING) != state.getValue(FACING)) {
            updateNeighborsInFront(level, pos, previous);
            level.neighborChanged(pos, (net.minecraft.world.level.block.Block)(Object)this, pos);
        }
        super.onPlace(state, level, pos, previous, moving);
    }
}

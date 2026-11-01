package design.inherent.createskycompat.mixin.linear;

import design.inherent.createskycompat.linear.LinearDisassembly;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.bearing.linearbearing.blocks.LinearCasingBlock", remap = false)
public abstract class LinearCasingBlockMixin extends Block {
    protected LinearCasingBlockMixin(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return InteractionResult.PASS;
        return level instanceof ServerLevel server ? LinearDisassembly.disassemble(server, pos, state, player)
                : InteractionResult.SUCCESS;
    }
}

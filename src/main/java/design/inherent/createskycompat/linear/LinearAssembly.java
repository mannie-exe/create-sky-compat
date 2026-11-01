package design.inherent.createskycompat.linear;

import com.bearing.linearbearing.blocks.LinearBearingBlock;
import com.bearing.linearbearing.blocks.LinearCasingBlock;
import com.bearing.linearbearing.blocks.LinearMovingBlock;
import com.bearing.linearbearing.registrate.ModBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Converts the original bearing only after native assembly and carriage placement succeed. */
public final class LinearAssembly {
    private LinearAssembly() {}

    public static InteractionResult assemble(ServerLevel level, BlockPos position, BlockState state, Player player) {
        var face = state.getValue(BlockStateProperties.ATTACH_FACE);
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var normal = switch (face) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> facing;
        };
        var front = position.relative(normal);
        boolean placeholder = level.getBlockState(front).isAir();
        SimAssemblyHelper.AssemblyResult result = null;
        if (placeholder && !level.setBlock(front, Blocks.STONE.defaultBlockState(), 3)) return InteractionResult.FAIL;
        try {
            result = SimAssemblyHelper.assembleFromSingleBlock(level, position, front, false, false);
        } catch (AssemblyException refusal) {
            player.displayClientMessage(refusal.component, true);
            return InteractionResult.FAIL;
        } finally {
            if (placeholder && result == null && level.getBlockState(front).is(Blocks.STONE))
                level.setBlock(front, Blocks.AIR.defaultBlockState(), 3);
        }
        if (result == null || result.subLevel() == null) return InteractionResult.FAIL;
        var movingPosition = position.offset(result.offset());
        if (Sable.HELPER.getContaining(level, movingPosition) != result.subLevel()
                || !level.getBlockState(movingPosition).isAir() || level.getBlockState(position) != state) {
            player.displayClientMessage(Component.translatable("create_sky_compat.linear.carriage_obstructed"), true);
            return InteractionResult.FAIL;
        }
        boolean back = state.getValue(LinearBearingBlock.SIDE) == LinearBearingBlock.SliderSide.BACK;
        var moving = ModBlocks.LINEAR_MOVING.get().defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, face)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(LinearMovingBlock.SIDE, back ? LinearMovingBlock.SliderSide.BACK : LinearMovingBlock.SliderSide.FRONT);
        if (!level.setBlock(movingPosition, moving, 3)) return InteractionResult.FAIL;
        if (placeholder) {
            var movedPlaceholder = front.offset(result.offset());
            if (level.getBlockState(movedPlaceholder).is(Blocks.STONE))
                level.setBlock(movedPlaceholder, Blocks.AIR.defaultBlockState(), 3);
        }
        var casing = ModBlocks.LINEAR_CASING.get().defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, face)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(LinearCasingBlock.SIDE, back ? LinearCasingBlock.SliderSide.BACK : LinearCasingBlock.SliderSide.FRONT);
        if (!level.setBlock(position, casing, 3)) return InteractionResult.FAIL;
        level.playSound(null, position, SoundEvent.createVariableRangeEvent(
                ResourceLocation.parse("simulated:block.physics_assembler.assemble")), SoundSource.BLOCKS, 1, 1);
        return InteractionResult.SUCCESS;
    }
}

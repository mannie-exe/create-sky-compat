package design.inherent.createskycompat.linear;

import com.bearing.linearbearing.blockentity.LinearCasingBlockEntity;
import com.bearing.linearbearing.blocks.LinearBearingBlock;
import com.bearing.linearbearing.blocks.LinearCasingBlock;
import com.bearing.linearbearing.registrate.ModBlocks;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaterniond;

/** Uses native block/entity transfer only after a read-only destination preflight. */
public final class LinearDisassembly {
    private static final double ALIGNMENT = Math.cos(Math.toRadians(1) / 2);
    private LinearDisassembly() {}

    public static InteractionResult disassemble(ServerLevel level, BlockPos position, BlockState casing, Player player) {
        if (!(level.getBlockEntity(position) instanceof LinearCasingBlockEntity rail)) return InteractionResult.PASS;
        var carriages = ((BrakeRail) rail).createSky$carriages();
        if (carriages.size() != 1) return refuse(player, "brake_required");
        var moving = carriages.getFirst();
        var brake = ((BrakedCarriage) moving).createSky$brake();
        if (brake == null || !brake.valid(moving) || brake.casing() != rail) return refuse(player, "brake_required");
        var own = brake.carriageOwner();
        var parent = brake.casingOwner();
        var relative = parent == null ? new Quaterniond() : new Quaterniond(parent.logicalPose().orientation());
        relative.conjugate().mul(own.logicalPose().orientation()).normalize();
        int turns = -1;
        for (int i = 0; i < 4; i++)
            if (Math.abs(relative.dot(new Quaterniond().rotationY(i * Math.PI / 2))) >= ALIGNMENT) { turns = i; break; }
        if (turns < 0) return refuse(player, "align_before_disassembly");
        var rotation = SimAssemblyHelper.rotationFrom90DegRots(turns);
        var transform = new SubLevelAssemblyHelper.AssemblyTransform(moving.getBlockPos(), position, turns, rotation, level);
        boolean hasCargo = false;
        for (var chunk : own.getPlot().getLoadedChunks()) {
            var bounds = chunk.getBoundingBox();
            if (bounds == null) continue;
            for (int x = bounds.minX(); x <= bounds.maxX(); x++)
                for (int y = bounds.minY(); y <= bounds.maxY(); y++)
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        var from = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                        if (from.equals(moving.getBlockPos()) || level.getBlockState(from).isAir()) continue;
                        var target = transform.apply(from);
                        if (level.isOutsideBuildHeight(target) || !level.isLoaded(target) || !level.mayInteract(player, target)
                                || Sable.HELPER.getContaining(level, target) != parent || !level.getBlockState(target).isAir())
                            return refuse(player, "disassembly_obstructed");
                        hasCargo = true;
                    }
        }
        if (!level.mayInteract(player, position)) return InteractionResult.FAIL;
        var bearing = ModBlocks.LINEAR_BEARING.get().defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, casing.getValue(BlockStateProperties.ATTACH_FACE))
                .setValue(BlockStateProperties.HORIZONTAL_FACING, casing.getValue(BlockStateProperties.HORIZONTAL_FACING))
                .setValue(LinearBearingBlock.SIDE, casing.getValue(LinearCasingBlock.SIDE) == LinearCasingBlock.SliderSide.BACK
                        ? LinearBearingBlock.SliderSide.BACK : LinearBearingBlock.SliderSide.FRONT);
        ((BrakedCarriage) moving).createSky$releaseBrake();
        level.setBlock(moving.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
        if (hasCargo) SimAssemblyHelper.disassembleSubLevel(level, own, moving.getBlockPos(), position, rotation, true);
        level.setBlock(position, bearing, 3);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuse(Player player, String key) {
        player.displayClientMessage(Component.translatable("create_sky_compat.linear." + key), true);
        return InteractionResult.FAIL;
    }
}

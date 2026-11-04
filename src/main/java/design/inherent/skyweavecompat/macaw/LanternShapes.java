package design.inherent.skyweavecompat.macaw;

import com.mcwbridges.kikoz.objects.items.Bridge_Torch;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Outline bounds follow the resource model; native empty collision stays untouched. */
public final class LanternShapes {
    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        double x0 = LanternLayout.MIN_X, x1 = LanternLayout.MAX_X;
        double z0 = LanternLayout.MIN_Z, z1 = LanternLayout.MAX_Z;
        for (int turn = 0; turn < 4; turn++) {
            SHAPES[turn] = Block.box(x0, LanternLayout.BOTTOM, z0, x1, LanternLayout.TOP, z1);
            double nextX0 = 16 - z1, nextX1 = 16 - z0;
            z0 = x0; z1 = x1; x0 = nextX0; x1 = nextX1;
        }
    }

    public static VoxelShape stair(BlockState state) {
        if (state.getValue(Bridge_Torch.LIGHTSTATE) != Bridge_Torch.LightState.STAIR) return null;
        Direction facing = state.getValue(Bridge_Torch.FACING);
        return SHAPES[switch (facing) { case EAST -> 0; case SOUTH -> 1; case WEST -> 2; default -> 3; }];
    }
    private LanternShapes() {}
}

package design.inherent.skyweavecompat.macaw;

import com.mcwbridges.kikoz.objects.items.Bridge_Torch;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Outline bounds follow the resource model; native empty collision stays untouched. */
public final class LanternShapes {
    private static final VoxelShape[] HANGING = rotations(LanternLayout.MIN_X, LanternLayout.BOTTOM,
            LanternLayout.MIN_Z, LanternLayout.MAX_X, LanternLayout.TOP, LanternLayout.MAX_Z);
    private static final VoxelShape[] STANDING = rotations(LanternLayout.FLAT_MIN_X, LanternLayout.FLAT_BOTTOM,
            LanternLayout.FLAT_MIN_Z, LanternLayout.FLAT_MAX_X, LanternLayout.FLAT_TOP, LanternLayout.FLAT_MAX_Z);

    private static VoxelShape[] rotations(double x0, double bottom, double z0, double x1, double top, double z1) {
        var shapes = new VoxelShape[4];
        for (int turn = 0; turn < 4; turn++) {
            shapes[turn] = Block.box(x0, bottom, z0, x1, top, z1);
            double nextX0 = 16 - z1, nextX1 = 16 - z0;
            z0 = x0; z1 = x1; x0 = nextX0; x1 = nextX1;
        }
        return shapes;
    }

    public static VoxelShape outline(BlockState state) {
        var shapes = state.getValue(Bridge_Torch.LIGHTSTATE) == Bridge_Torch.LightState.STAIR ? HANGING : STANDING;
        Direction facing = state.getValue(Bridge_Torch.FACING);
        return shapes[switch (facing) { case EAST -> 0; case SOUTH -> 1; case WEST -> 2; default -> 3; }];
    }
    private LanternShapes() {}
}

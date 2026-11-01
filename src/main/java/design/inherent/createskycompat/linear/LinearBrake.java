package design.inherent.createskycompat.linear;

import com.bearing.linearbearing.blockentity.LinearCasingBlockEntity;
import com.bearing.linearbearing.blocks.LinearCasingBlock;
import com.bearing.linearbearing.blocks.LinearMovingBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import design.inherent.createskycompat.mixin.linear.DockingBrakeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Captures the current relative pose without snapping or pairing connector inventories. */
public record LinearBrake(FixedConstraintHandle handle, LinearCasingBlockEntity casing,
                          ServerSubLevel carriageOwner, SubLevel casingOwner) {
    public static LinearBrake attach(DockingConnectorBlockEntity carriage) {
        if (!(carriage.getLevel() instanceof ServerLevel level)
                || !(Sable.HELPER.getContaining(carriage) instanceof ServerSubLevel own)) return null;
        if (!RigidBodyHandle.of(own).isValid()) return null;
        Vec3 world = Sable.HELPER.projectOutOfSubLevel(level, carriage.getBlockPos().getCenter());
        var casing = casingAt(level, BlockPos.containing(world));
        if (casing == null) {
            var bounds = new BoundingBox3d(world.x - 0.01, world.y - 0.01, world.z - 0.01,
                    world.x + 0.01, world.y + 0.01, world.z + 0.01);
            for (var body : Sable.HELPER.getAllIntersecting(level, bounds)) {
                if (body == own) continue;
                var local = body.logicalPose().transformPositionInverse(world);
                casing = casingAt(level, BlockPos.containing(local));
                if (casing != null) break;
            }
        }
        if (casing == null) return null;
        var other = Sable.HELPER.getContaining(casing);
        if (other == own || other != null && !(other instanceof ServerSubLevel)
                || !aligned(carriage.getBlockState(), casing.getBlockState(), own, other)) return null;
        if (other != null && !RigidBodyHandle.of((ServerSubLevel) other).isValid()) return null;
        var localAnchor = new Vector3d(carriage.getBlockPos().getX() + 0.5,
                carriage.getBlockPos().getY() + 0.5, carriage.getBlockPos().getZ() + 0.5);
        var otherAnchor = new Vector3d(world.x, world.y, world.z);
        if (other != null) other.logicalPose().transformPositionInverse(otherAnchor);
        var relative = new Quaterniond(own.logicalPose().orientation()).conjugate();
        if (other != null) relative.mul(other.logicalPose().orientation());
        var config = new FixedConstraintConfiguration(localAnchor, otherAnchor, relative);
        var container = SubLevelContainer.getContainer(level);
        var handle = container.physicsSystem().getPipeline().addConstraint(own, (ServerSubLevel) other, config);
        ((DockingBrakeAccessor) carriage).createSky$brakeHandle(handle);
        ((BrakeRail) casing).createSky$addCarriage(carriage);
        return new LinearBrake(handle, casing, own, other);
    }

    public boolean valid(DockingConnectorBlockEntity carriage) {
        return handle.isValid() && !casing.isRemoved() && carriage.getLevel() == casing.getLevel()
                && Sable.HELPER.getContaining(carriage) == carriageOwner
                && Sable.HELPER.getContaining(casing) == casingOwner
                && casing.getLevel().isLoaded(casing.getBlockPos())
                && aligned(carriage.getBlockState(), casing.getBlockState(), carriageOwner, casingOwner)
                && casing.getLevel().getBlockEntity(casing.getBlockPos()) == casing;
    }

    private static boolean aligned(BlockState carriage, BlockState casing, SubLevel own, SubLevel other) {
        return axis(carriage, own, true, true).dot(axis(casing, other, false, true)) > 0.999
                && Math.abs(axis(carriage, own, true, false).dot(axis(casing, other, false, false))) > 0.999;
    }

    private static Vector3d axis(BlockState state, SubLevel body, boolean moving, boolean normal) {
        var face = state.getValue(BlockStateProperties.ATTACH_FACE);
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean back = moving ? state.getValue(LinearMovingBlock.SIDE) == LinearMovingBlock.SliderSide.BACK
                : state.getValue(LinearCasingBlock.SIDE) == LinearCasingBlock.SliderSide.BACK;
        var direction = normal ? switch (face) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> facing;
        } : face == AttachFace.WALL ? back ? facing.getClockWise() : Direction.UP : facing;
        var result = new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        return body == null ? result : body.logicalPose().transformNormal(result);
    }

    private static LinearCasingBlockEntity casingAt(ServerLevel level, BlockPos position) {
        return level.isLoaded(position) && level.getBlockEntity(position) instanceof LinearCasingBlockEntity casing
                && !casing.isRemoved() ? casing : null;
    }
}

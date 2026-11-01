package design.inherent.createskycompat.linear;

import com.bearing.linearbearing.blockentity.MagneticPortBlockEntity;
import com.bearing.linearbearing.blockentity.TorsionalAnchorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/** Only discovers edges; Create owns their speed, source, capacity, and load accounting. */
public final class TransmissionLinks {
    private TransmissionLinks() {}

    public static void tick(KineticBlockEntity self) {
        var endpoint = (TransmissionEndpoint) self;
        var candidate = discover(self);
        if (candidate != null && (discover(candidate) != self || !compatibleSpeeds(self, candidate))) candidate = null;
        if (endpoint.createSky$peer() != candidate) {
            disconnect(self);
            if (candidate != null && ((TransmissionEndpoint) candidate).createSky$peer() == null) {
                self.detachKinetics(); candidate.detachKinetics();
                endpoint.createSky$peer(candidate);
                ((TransmissionEndpoint) candidate).createSky$peer(self);
                self.attachKinetics(); candidate.attachKinetics();
            }
        }
        if (self instanceof MagneticStatus status)
            status.createSky$linked(endpoint.createSky$peer() == null ? null : endpoint.createSky$peer().getBlockPos());
    }

    public static void disconnect(KineticBlockEntity self) {
        var endpoint = (TransmissionEndpoint) self;
        var previous = endpoint.createSky$peer();
        if (previous == null) return;
        // Remove the old edge while Create can still discover it for source invalidation.
        self.detachKinetics();
        endpoint.createSky$peer(null);
        if (previous instanceof TransmissionEndpoint other && other.createSky$peer() == self) {
            other.createSky$peer(null);
            if (!previous.isRemoved()) previous.attachKinetics();
        }
        if (!self.isRemoved()) self.attachKinetics();
    }

    public static boolean connected(KineticBlockEntity self, KineticBlockEntity other) {
        return other != null && !other.isRemoved() && self.getLevel() == other.getLevel()
                && ((TransmissionEndpoint) self).createSky$peer() == other
                && other instanceof TransmissionEndpoint endpoint && endpoint.createSky$peer() == self;
    }

    public static float ratio(KineticBlockEntity self, KineticBlockEntity other) {
        var a = self.getBlockState().getValue(BlockStateProperties.FACING);
        var b = other.getBlockState().getValue(BlockStateProperties.FACING);
        if (self instanceof TorsionalAnchorBlockEntity) return a == b.getOpposite() ? -1 : 1;
        return normal(self, Direction.fromAxisAndDirection(a.getAxis(), Direction.AxisDirection.POSITIVE))
                .dot(normal(other, Direction.fromAxisAndDirection(b.getAxis(), Direction.AxisDirection.POSITIVE))) < 0 ? -1 : 1;
    }

    private static boolean compatibleSpeeds(KineticBlockEntity a, KineticBlockEntity b) {
        float first = a.getTheoreticalSpeed(), second = b.getTheoreticalSpeed();
        return first == 0 || second == 0 || Math.abs(first * ratio(a, b) - second) < 0.001f;
    }

    private static KineticBlockEntity discover(KineticBlockEntity self) {
        if (self.isRemoved() || self.getLevel() == null || self.getLevel().isClientSide()) return null;
        return self instanceof MagneticPortBlockEntity ? magnetic(self) : spring(self);
    }

    private static KineticBlockEntity magnetic(KineticBlockEntity self) {
        var level = self.getLevel();
        var facing = self.getBlockState().getValue(BlockStateProperties.FACING);
        var direction = normal(self, facing);
        Vec3 start = Sable.HELPER.projectOutOfSubLevel(level,
                self.getBlockPos().getCenter().add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.51)));
        Vec3 end = start.add(direction.x * 3.5, direction.y * 3.5, direction.z * 3.5);
        for (int i = 0; i <= 14; i++)
            if (!level.isLoaded(BlockPos.containing(start.lerp(end, i / 14.0)))) return null;
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        if (hit.getType() != HitResult.Type.BLOCK || !level.isLoaded(hit.getBlockPos())) return null;
        var block = level.getBlockEntity(hit.getBlockPos());
        if (!(block instanceof MagneticPortBlockEntity target) || target == self
                || !(target instanceof TransmissionEndpoint) || target.isRemoved()) return null;
        var targetFacing = target.getBlockState().getValue(BlockStateProperties.FACING);
        return targetFacing == hit.getDirection() && direction.dot(normal(target, targetFacing)) < -0.999 ? target : null;
    }

    private static KineticBlockEntity spring(KineticBlockEntity self) {
        var level = self.getLevel();
        var front = self.getBlockPos().relative(self.getBlockState().getValue(BlockStateProperties.FACING));
        if (!level.isLoaded(front) || !(level.getBlockEntity(front) instanceof SpringBlockEntity first)) return null;
        var partner = first.getPairedSpring();
        if (partner == null || partner.isRemoved() || partner.getLevel() != level || partner.getPairedSpring() != first) return null;
        var ship = Sable.HELPER.getContaining(partner);
        if (!java.util.Objects.equals(first.getPartnerSubLevelID(), ship == null ? null : ship.getUniqueId())) return null;
        for (var direction : Direction.values()) {
            var position = partner.getBlockPos().relative(direction);
            if (!level.isLoaded(position)) continue;
            if (level.getBlockEntity(position) instanceof TorsionalAnchorBlockEntity target
                    && target instanceof TransmissionEndpoint && target != self && !target.isRemoved()
                    && position.relative(target.getBlockState().getValue(BlockStateProperties.FACING)).equals(partner.getBlockPos()))
                return target;
        }
        return null;
    }

    private static Vector3d normal(BlockEntity block, Direction direction) {
        var result = new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        var ship = Sable.HELPER.getContaining(block);
        return ship == null ? result : ship.logicalPose().transformNormal(result);
    }
}

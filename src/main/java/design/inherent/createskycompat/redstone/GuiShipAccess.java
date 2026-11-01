package design.inherent.createskycompat.redstone;

import dev.ryanhcode.sable.Sable;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Loaded only after the separate Sable metadata gate succeeds. */
final class GuiShipAccess {
    static Vec3 project(Level level, BlockPos pos) { return Sable.HELPER.projectOutOfSubLevel(level, pos.getCenter()); }
    static UUID frame(Level level, BlockPos pos) {
        var ship = Sable.HELPER.getContaining(level, pos);
        return ship == null || ship.isRemoved() ? null : ship.getUniqueId();
    }
    private GuiShipAccess() {}
}

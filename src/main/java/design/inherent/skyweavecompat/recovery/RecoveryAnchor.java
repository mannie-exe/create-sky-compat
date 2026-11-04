package design.inherent.skyweavecompat.recovery;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** Placement metadata only. Inventory and claim authority remain in YiGD. */
public record RecoveryAnchor(String dimension, UUID construct, BlockPos local, Vec3 fallback, String state) {
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("version", 1);
        tag.putString("dimension", dimension);
        if (construct != null) tag.putUUID("construct", construct);
        tag.putLong("local", local.asLong());
        tag.putDouble("x", fallback.x); tag.putDouble("y", fallback.y); tag.putDouble("z", fallback.z);
        tag.putString("state", state);
        return tag;
    }

    public static RecoveryAnchor read(CompoundTag tag) {
        if (tag.getInt("version") != 1) return null;
        var point = new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
        if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)) return null;
        return new RecoveryAnchor(tag.getString("dimension"), tag.hasUUID("construct") ? tag.getUUID("construct") : null,
                BlockPos.of(tag.getLong("local")), point, tag.getString("state"));
    }

    public RecoveryAnchor state(String value) { return new RecoveryAnchor(dimension, construct, local, fallback, value); }
}

package design.inherent.skyweavecompat.recovery;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Protection extensions may reject the actual plot target and projected world target together. */
public final class RecoveryPlacementEvent extends Event implements ICancellableEvent {
    public final ServerPlayer owner;
    public final ServerLevel level;
    public final BlockPos physicalPosition;
    public final BlockPos worldPosition;

    public RecoveryPlacementEvent(ServerPlayer owner, ServerLevel level, BlockPos physicalPosition, BlockPos worldPosition) {
        this.owner = owner; this.level = level;
        this.physicalPosition = physicalPosition; this.worldPosition = worldPosition;
    }
}

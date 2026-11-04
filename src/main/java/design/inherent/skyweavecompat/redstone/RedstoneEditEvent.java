package design.inherent.skyweavecompat.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Optional claim integrations may veto a link edit or relocation endpoint. */
public final class RedstoneEditEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final BlockEntity source;
    private final BlockPos position, worldPosition;
    private final boolean relocating;
    public RedstoneEditEvent(ServerPlayer player, BlockEntity source, BlockPos position, BlockPos worldPosition, boolean relocating) {
        this.player = player; this.source = source; this.position = position.immutable(); this.worldPosition = worldPosition.immutable(); this.relocating = relocating;
    }
    public ServerPlayer player() { return player; }
    public BlockEntity source() { return source; }
    public BlockPos position() { return position; }
    public BlockPos worldPosition() { return worldPosition; }
    public boolean relocating() { return relocating; }
}

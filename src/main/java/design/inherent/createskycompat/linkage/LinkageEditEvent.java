package design.inherent.createskycompat.linkage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Server authorization hook for a real pending endpoint edit, before any mutation. */
public final class LinkageEditEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final BlockEntity endpoint;
    private final BlockPos worldPosition;
    private final boolean removing;

    public LinkageEditEvent(ServerPlayer player, BlockEntity endpoint, BlockPos worldPosition, boolean removing) {
        this.player = player;
        this.endpoint = endpoint;
        this.worldPosition = worldPosition.immutable();
        this.removing = removing;
    }
    public ServerPlayer player() { return player; }
    public BlockEntity endpoint() { return endpoint; }
    public BlockPos worldPosition() { return worldPosition; }
    public boolean removing() { return removing; }
}

package design.inherent.skyweavecompat.redstone;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The target and owner captured when the server creates a menu. */
public interface GuiMenuState {
    BlockEntity skyweave$initialTarget();
    Player skyweave$owner();
    UUID skyweave$frame();
}

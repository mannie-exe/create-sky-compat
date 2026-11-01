package design.inherent.createskycompat.redstone;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The target and owner captured when the server creates a menu. */
public interface GuiMenuState {
    BlockEntity createSky$initialTarget();
    Player createSky$owner();
    UUID createSky$frame();
}

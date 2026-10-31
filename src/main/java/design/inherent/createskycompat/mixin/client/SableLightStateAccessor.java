package design.inherent.createskycompat.mixin.client;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** SDL owns this cache even when the Sable mod is absent. */
@Mixin(targets = "fr0nick.sabledynlights.lamb.sable.SableSubLevelEventHandler", remap = false)
public interface SableLightStateAccessor {
    @Accessor("lastLevel") static void createSky$lastLevel(Level level) { throw new AssertionError(); }
    @Accessor("lastScanMillis") static void createSky$lastScan(long time) { throw new AssertionError(); }
}

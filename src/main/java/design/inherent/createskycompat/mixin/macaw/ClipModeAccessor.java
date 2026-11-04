package design.inherent.createskycompat.mixin.macaw;

import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClipContext.class)
public interface ClipModeAccessor {
    @Accessor("block") ClipContext.Block createSky$blockMode();
}

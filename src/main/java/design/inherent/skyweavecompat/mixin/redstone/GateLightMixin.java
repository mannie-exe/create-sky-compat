package design.inherent.skyweavecompat.mixin.redstone;

import dev.siliconcarbidecube.create_redstone_additions.blocks.Crossroad;
import dev.siliconcarbidecube.create_redstone_additions.util.LightLevels;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LightLevels.class, remap = false)
public abstract class GateLightMixin {
    @Inject(method = "computeLightLevel", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void skyweave$bothAxes(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (state.hasProperty(Crossroad.FLANK_POWER)) cir.setReturnValue(Math.max(state.getValue(Crossroad.POWER), state.getValue(Crossroad.FLANK_POWER)));
    }
}

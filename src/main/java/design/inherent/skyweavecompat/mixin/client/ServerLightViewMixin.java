package design.inherent.skyweavecompat.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client render views remain eligible; authoritative server views keep their own light. */
@Mixin(targets = "fr0nick.sabledynlights.util.WorldPosResolver", remap = false)
public abstract class ServerLightViewMixin {
    @Inject(method = "getCombinedWorldLight", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void skyweave$serverLight(BlockAndTintGetter view, BlockPos position, int current,
            CallbackInfoReturnable<Integer> result) {
        if (view instanceof LevelAccessor level && !level.isClientSide()) result.setReturnValue(current);
    }
}

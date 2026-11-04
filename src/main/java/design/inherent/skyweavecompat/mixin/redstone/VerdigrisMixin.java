package design.inherent.skyweavecompat.mixin.redstone;

import dev.siliconcarbidecube.create_redstone_additions.interacts.AddInteracts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace the early right-click probe with the native tool-modification event. */
@Mixin(value = AddInteracts.class, remap = false)
public abstract class VerdigrisMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void skyweave$nativeScraping(CallbackInfo ci) { ci.cancel(); }
}

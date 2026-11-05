package design.inherent.skyweavecompat.mixin.client;

import design.inherent.skyweavecompat.client.ShaderAssembly;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native patcher may inspect owned outputs, but must not modify their verified contents. */
@Pseudo
@Mixin(targets = "com.euphoriapatches.euphoria_patcher.util.shader.BootShaderModificator", remap = false)
public abstract class EuphoriaOwnedOutputMixin {
    @Inject(method = "applyShaderModifications(Ljava/nio/file/Path;ZZZ)V", at = @At("HEAD"), cancellable = true, require = 1)
    private static void skyweave$keepOwned(Path path, boolean a, boolean b, boolean c, CallbackInfo callback) {
        if (ShaderAssembly.owned(path)) callback.cancel();
    }
}

package design.inherent.skyweavecompat.mixin.client;

import design.inherent.skyweavecompat.client.ShaderAssembly;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Assembly precedes the pinned patcher's scanner and directory watcher. Runtime integrations remain native. */
@Pseudo
@Mixin(targets = "com.euphoriapatches.euphoria_patcher.EuphoriaPatcher", remap = false)
public abstract class EuphoriaAssemblyMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/euphoriapatches/euphoria_patcher/EuphoriaPatcher;initializeServices()V"), require = 1)
    private void skyweave$beforeScanner(CallbackInfo callback) { ShaderAssembly.prepare(); }
}

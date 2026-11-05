package design.inherent.skyweavecompat.mixin.client;

import design.inherent.skyweavecompat.client.ShaderAssembly;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extend native detection with receipt-verified outputs; native unrelated style detection still runs. */
@Pseudo
@Mixin(targets = "com.euphoriapatches.euphoria_patcher.services.ShaderDetector", remap = false)
public abstract class EuphoriaDetectorMixin {
    @Inject(method = "checkForExistingPatchedShaders", at = @At("RETURN"), require = 1)
    private void skyweave$recognize(@Coerce Object info, CallbackInfo callback) {
        var output = ShaderAssembly.euphoriaOutput();
        if (output == null) return;
        try {
            Class<?> type = info.getClass();
            if (!type.getField("isAlreadyInstalled").getBoolean(info)) {
                type.getField("isAlreadyInstalled").setBoolean(info, true);
                type.getField("baseFile").set(info, output);
                type.getField("installedDir").set(info, output);
            }
            type.getField("styleUnbound").setBoolean(info, true);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Changed Euphoria detection contract", failure); }
    }
}

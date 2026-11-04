package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.djefrey.colorwheel.util.GlCompat;
import java.util.Set;
import java.util.stream.Collectors;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Unsupported OIT never binds its textures; do not starve Iris of those unused units. */
@Mixin(targets = "dev.djefrey.colorwheel.compile.ClrwlProgram", remap = false)
public abstract class ColorwheelSamplerMixin {
    // Colorwheel 1.3.0 reserves base slots 0..6, OIT depth/noise 7..8 and coefficients 9+.
    @Unique private static final int SKYWEAVE_FIRST_OIT_UNIT = 7;

    @WrapOperation(method = "postLink", at = @At(value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/program/ProgramSamplers;builder(ILjava/util/Set;)Lnet/irisshaders/iris/gl/program/ProgramSamplers$Builder;"))
    private ProgramSamplers.Builder skyweave$availableSamplers(int program, Set<Integer> reserved,
            Operation<ProgramSamplers.Builder> original) {
        if (!GlCompat.SUPPORTS_OIT) reserved = reserved.stream()
                .filter(unit -> unit < SKYWEAVE_FIRST_OIT_UNIT).collect(Collectors.toSet());
        return original.call(program, reserved);
    }

    @WrapOperation(method = "postLink", at = @At(value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/program/ProgramSamplers$Builder;addExternalSampler(I[Ljava/lang/String;)V"))
    private void skyweave$unusedOitSampler(ProgramSamplers.Builder builder, int unit, String[] names,
            Operation<Void> original) {
        if (GlCompat.SUPPORTS_OIT || unit < SKYWEAVE_FIRST_OIT_UNIT) original.call(builder, unit, names);
    }
}

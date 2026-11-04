package design.inherent.skyweavecompat.mixin.linear;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.bearing.linearbearing.blockentity.GyroscopicGearboxBlockEntity", remap = false)
public abstract class GyroSensorMixin {
    // Math.round(+0.5) otherwise traps a decaying positive sample at 0.5 RPM.
    @ModifyArg(method = "processDirectGeneratorPhysics", at = @At(value = "INVOKE",
            target = "Ljava/lang/Math;round(F)I"), index = 0)
    private float skyweave$settleStationarySample(float sample) {
        return Math.abs(sample) <= 0.5f ? 0 : sample;
    }

    @Inject(method = "calculateAddedStressCapacity", at = @At("HEAD"), cancellable = true)
    private void skyweave$signalWithoutPower(CallbackInfoReturnable<Float> callback) { callback.setReturnValue(0f); }
}

package design.inherent.createskycompat.mixin.audio;

import com.palm1.analogaudio.client.ClientHooks;
import com.palm1.analogaudio.item.CassetteData;
import design.inherent.createskycompat.audio.RadioSessions;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientHooks.class, remap = false)
public abstract class RadioTimelineMixin {
    @Inject(method = "tickRadio", at = @At("HEAD"))
    private static void createSky$sample(Object identity, Vec3 pos, CassetteData data, long startTime,
            float volume, boolean looping, CallbackInfo ci) {
        if (pos != null && data != null)
            RadioSessions.touch(identity, data.uuid(), data.url(), startTime, data.duration(), looping);
    }

    @Inject(method = "stopRadio", at = @At("TAIL"))
    private static void createSky$remove(Object identity, CallbackInfo ci) { RadioSessions.remove(identity); }
}

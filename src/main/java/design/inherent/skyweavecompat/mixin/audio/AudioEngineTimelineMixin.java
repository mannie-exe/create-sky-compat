package design.inherent.skyweavecompat.mixin.audio;

import com.palm1.analoglib.client.audio.ClientAudioEngine;
import com.palm1.analoglib.client.audio.api.IRadioStreamer;
import com.palm1.analoglib.client.audio.engine.EmitterState;
import com.palm1.analoglib.client.audio.engine.SpatialStreamClusterer;
import com.palm1.analoglib.client.audio.engine.StreamSessionManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import design.inherent.skyweavecompat.audio.RadioSessions;
import design.inherent.skyweavecompat.audio.TimelineStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientAudioEngine.class, remap = false)
public abstract class AudioEngineTimelineMixin {
    @Shadow @Final private static StreamSessionManager SESSION_MANAGER;

    @WrapOperation(method = "tickStreamInternal", at = @At(value = "INVOKE", target =
            "Lcom/palm1/analoglib/client/audio/ClientAudioEngine$ServerAudioTokenResolver;requestToken(Ljava/lang/Object;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private static void skyweave$guardToken(ClientAudioEngine.ServerAudioTokenResolver resolver,
            Object streamKey, String requestUrl, Consumer<String> callback, Operation<Void> original,
            Object key, String uuid, String url, long start, long duration, boolean looping,
            boolean overlap, List<EmitterState> emitters, EmitterState closest) {
        var clock = RadioSessions.find(uuid, start, emitters);
        if (clock == null) { original.call(resolver, streamKey, requestUrl, callback); return; }
        original.call(resolver, streamKey, requestUrl, (Consumer<String>) token -> Minecraft.getInstance().execute(() -> {
            if (clock.valid() && RadioSessions.find(uuid, start, emitters) == clock) callback.accept(token);
        }));
    }

    @WrapOperation(method = "tickRadio(Ljava/lang/Object;Lnet/minecraft/world/phys/Vec3;Ljava/lang/String;Ljava/lang/String;JJFZZDFFZFFZ)V", at = @At(value = "INVOKE",
            target = "Lcom/palm1/analoglib/client/audio/engine/SpatialStreamClusterer;resolveSpatialStreamKey(Ljava/lang/String;JLjava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;"))
    private static Object skyweave$sessionKey(SpatialStreamClusterer clusterer, String group, long start,
            Object identity, Map<Object, Long> standby, Operation<Object> original) {
        return original.call(clusterer, RadioSessions.registered(identity) ? group + "_radio_" + start : group,
                start, identity, standby);
    }

    @Inject(method = "tickStreamInternal", at = @At("HEAD"))
    private static void skyweave$refresh(Object key, String uuid, String url, long start, long duration,
            boolean looping, boolean overlap, List<EmitterState> emitters, EmitterState closest, CallbackInfo ci) {
        var entry = SESSION_MANAGER.getPlaying(key);
        if (entry != null && entry.streamer() instanceof TimelineStream stream) {
            var clock = RadioSessions.find(uuid, start, emitters);
            if (clock != null) stream.skyweave$timeline(clock);
        }
    }

    @WrapOperation(method = "startPlayback", at = @At(value = "INVOKE",
            target = "Lcom/palm1/analoglib/client/audio/api/IRadioStreamer;playTrack(Ljava/lang/String;JJ)V"))
    private static void skyweave$bindBeforeLoading(IRadioStreamer streamer, String url, long offset,
            long trackDuration, Operation<Void> original, Object key, Vec3 pos, String uuid, String inputUrl,
            long start, long duration, boolean looping, boolean overlap, List<EmitterState> emitters, EmitterState closest) {
        if (streamer instanceof TimelineStream stream) stream.skyweave$timeline(RadioSessions.find(uuid, start, emitters));
        original.call(streamer, url, offset, trackDuration);
    }

    @WrapOperation(method = "startPlayback", at = @At(value = "INVOKE",
            target = "Lcom/palm1/analoglib/client/audio/api/IRadioStreamer;resume()V"))
    private static void skyweave$bindBeforeResume(IRadioStreamer streamer, Operation<Void> original,
            Object key, Vec3 pos, String uuid, String url, long start, long duration, boolean looping,
            boolean overlap, List<EmitterState> emitters, EmitterState closest) {
        if (streamer instanceof TimelineStream stream) stream.skyweave$timeline(RadioSessions.find(uuid, start, emitters));
        original.call(streamer);
    }

    @WrapOperation(method = "startPlayback", at = @At(value = "INVOKE",
            target = "Lcom/palm1/analoglib/client/audio/api/IRadioStreamer;setOnError(Ljava/util/function/Consumer;)V"))
    private static void skyweave$errorOnGameThread(IRadioStreamer streamer, Consumer<String> callback,
            Operation<Void> original, Object key, Vec3 pos, String uuid, String url, long start, long duration,
            boolean looping, boolean overlap, List<EmitterState> emitters, EmitterState closest) {
        var clock = RadioSessions.find(uuid, start, emitters);
        if (clock == null) { original.call(streamer, callback); return; }
        original.call(streamer, (Consumer<String>) error -> Minecraft.getInstance().execute(() -> {
            if (clock.valid() && RadioSessions.find(uuid, start, emitters) == clock) callback.accept(error);
        }));
    }

    @WrapOperation(method = "startPlayback", at = @At(value = "INVOKE",
            target = "Lcom/palm1/analoglib/client/audio/api/IRadioStreamer;setOnTrackEnd(Ljava/lang/Runnable;)V"))
    private static void skyweave$completionOnGameThread(IRadioStreamer streamer, Runnable callback,
            Operation<Void> original, Object key, Vec3 pos, String uuid, String url, long start, long duration,
            boolean looping, boolean overlap, List<EmitterState> emitters, EmitterState closest) {
        var clock = RadioSessions.find(uuid, start, emitters);
        if (clock == null) { original.call(streamer, callback); return; }
        original.call(streamer, (Runnable) () -> Minecraft.getInstance().execute(() -> {
            if (clock.valid() && RadioSessions.find(uuid, start, emitters) == clock) callback.run();
        }));
    }

    @Inject(method = "stopAll", at = @At("TAIL"))
    private static void skyweave$clear(String trigger, boolean deactivate, CallbackInfo ci) { RadioSessions.clear(); }
}

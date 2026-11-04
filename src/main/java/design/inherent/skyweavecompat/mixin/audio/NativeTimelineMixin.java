package design.inherent.skyweavecompat.mixin.audio;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.palm1.analoglib.client.audio.nativeaudio.NativeRadioStreamer;
import com.palm1.analoglib.client.audio.openal.OpenALAudioSource;
import design.inherent.skyweavecompat.audio.PlaybackTimeline;
import design.inherent.skyweavecompat.audio.TimelineStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Correct only the queue cursor; preserve native decoding, spatial properties and gains. */
@Mixin(value = NativeRadioStreamer.class, remap = false)
public abstract class NativeTimelineMixin implements TimelineStream {
    @Shadow private long currentOffsetMs;
    @Shadow private volatile boolean playing;
    @Shadow private volatile boolean isPaused;
    @Unique private volatile PlaybackTimeline skyweave$clock;
    @Unique private volatile long skyweave$request;

    @Override public void skyweave$timeline(PlaybackTimeline clock) { skyweave$clock = clock; }

    @WrapMethod(method = "playTrack(Ljava/lang/String;JJ)V")
    private void skyweave$serializeReplacement(String url, long offset, long duration, Operation<Void> original) {
        if (skyweave$clock == null) { original.call(url, offset, duration); return; }
        synchronized (this) { original.call(url, offset, duration); }
    }

    @WrapMethod(method = "stop")
    private void skyweave$serializeStop(Operation<Void> original) {
        if (skyweave$clock == null) { original.call(); return; }
        synchronized (this) { original.call(); }
    }

    @WrapOperation(method = "playTrack(Ljava/lang/String;JJ)V", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/ExecutorService;submit(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;"))
    private Future<?> skyweave$guardResolver(ExecutorService executor, Runnable work, Operation<Future<?>> original) {
        PlaybackTimeline clock = skyweave$clock;
        if (clock == null) return original.call(executor, work);
        long request = ++skyweave$request;
        return original.call(executor, (Runnable) () -> {
            synchronized (this) {
                if (playing && skyweave$request == request && clock.valid()) work.run();
            }
        });
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void skyweave$stop(CallbackInfo ci) { skyweave$request++; skyweave$clock = null; }

    @WrapOperation(method = "streamingLoop", at = @At(value = "INVOKE",
            target = "Lcom/palm1/analoglib/client/audio/nativeaudio/NativeRadioStreamer;getMasterPlaybackChunkIndex()J"))
    private long skyweave$allowDecoderCatchup(NativeRadioStreamer streamer, Operation<Long> original) {
        long actual = original.call(streamer);
        PlaybackTimeline clock = skyweave$clock;
        return clock != null && clock.valid() && !isPaused
                ? Math.max(actual, clock.framesAfter(currentOffsetMs) / PlaybackTimeline.CHUNK_FRAMES) : actual;
    }

    @Inject(method = "pumpEmitter", at = @At("HEAD"), cancellable = true)
    private void skyweave$alignQueue(OpenALAudioSource emitter, CallbackInfo ci) {
        PlaybackTimeline clock = skyweave$clock;
        if (clock == null || isPaused) return;
        if (!clock.valid()) { ci.cancel(); return; }
        // AL_SAMPLE_OFFSET is relative to the whole remaining queue, including processed buffers.
        emitter.recycleBuffers();
        long desired = clock.framesAfter(currentOffsetMs);
        long actual = 0;
        if (emitter.sourceId != -1 && emitter.audioStarted) {
            int active = AL10.alGetSourcei(emitter.sourceId, AL10.AL_BUFFERS_QUEUED)
                    - AL10.alGetSourcei(emitter.sourceId, AL10.AL_BUFFERS_PROCESSED);
            actual = Math.max(0, emitter.nextChunkToQueue - Math.max(0, active)) * PlaybackTimeline.CHUNK_FRAMES
                    + Math.max(0, AL10.alGetSourcei(emitter.sourceId, AL11.AL_SAMPLE_OFFSET));
        }
        if (emitter.audioStarted && actual - desired > PlaybackTimeline.SAMPLE_RATE / 4) {
            AL10.alSourcePause(emitter.sourceId);
            if (emitter.sourceRightId != -1) AL10.alSourcePause(emitter.sourceRightId);
            ci.cancel();
            return;
        }
        if (!emitter.audioStarted || desired - actual > PlaybackTimeline.SAMPLE_RATE / 4) {
            if (emitter.sourceId != -1) {
                AL10.alSourceStop(emitter.sourceId);
                if (emitter.sourceRightId != -1) AL10.alSourceStop(emitter.sourceRightId);
                emitter.recycleBuffers();
            }
            emitter.audioStarted = false;
            emitter.nextChunkToQueue = desired / PlaybackTimeline.CHUNK_FRAMES;
            emitter.initialSampleOffset = (int) (desired % PlaybackTimeline.CHUNK_FRAMES);
        }
    }
}

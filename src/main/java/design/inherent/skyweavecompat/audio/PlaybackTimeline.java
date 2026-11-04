package design.inherent.skyweavecompat.audio;

/** Server media time sampled on the game thread; audio threads never read a level. */
public final class PlaybackTimeline {
    public static final int SAMPLE_RATE = 44100;
    public static final int CHUNK_FRAMES = 2048;
    private volatile long milliseconds;
    private volatile boolean valid = true;

    public void update(long gameTick, long startTick, long duration, boolean looping) {
        long elapsed = Math.max(0L, gameTick - startTick) * 50L;
        milliseconds = duration > 0 ? (looping ? elapsed % duration : Math.min(duration, elapsed)) : elapsed;
    }

    public long framesAfter(long decoderOffsetMs) {
        return Math.max(0L, milliseconds - decoderOffsetMs) * SAMPLE_RATE / 1000L;
    }

    public long milliseconds() { return milliseconds; }
    public boolean valid() { return valid; }
    public void invalidate() { valid = false; }
}

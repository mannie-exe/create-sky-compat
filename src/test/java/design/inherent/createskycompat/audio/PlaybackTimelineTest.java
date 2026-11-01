package design.inherent.createskycompat.audio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackTimelineTest {
    @Test void loadingTimeAdvancesTheQueueRatherThanTheSongOrigin() {
        var clock = new PlaybackTimeline();
        clock.update(100, 100, 180_000, false);
        assertEquals(0, clock.framesAfter(0));
        clock.update(400, 100, 180_000, false);
        assertEquals(15L * 44100, clock.framesAfter(0));
        assertEquals(5L * 44100, clock.framesAfter(10_000));
    }

    @Test void standbyUsesCurrentServerTimeAndPauseFreezesIt() {
        var clock = new PlaybackTimeline();
        clock.update(500, 100, 180_000, false);
        assertEquals(20_000, clock.milliseconds());
        assertEquals(20_000, clock.milliseconds());
        clock.update(700, 100, 180_000, false);
        assertEquals(30_000, clock.milliseconds());
    }

    @Test void boundariesRespectLoopDurationAndInvalidation() {
        var clock = new PlaybackTimeline();
        clock.update(50, 100, 1000, false);
        assertEquals(0, clock.milliseconds());
        clock.update(150, 100, 1000, false);
        assertEquals(1000, clock.milliseconds());
        clock.update(150, 100, 1000, true);
        assertEquals(500, clock.milliseconds());
        assertEquals(0, clock.framesAfter(800));
        clock.invalidate();
        assertFalse(clock.valid());
    }
}

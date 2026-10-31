package design.inherent.createskycompat.questlog;

import org.junit.jupiter.api.Test;
import static design.inherent.createskycompat.questlog.ProgressNoticeState.Notice.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgressNoticeStateTest {
    @Test void firstProgressOnlyAndCompletionWins() {
        var state = new ProgressNoticeState(false, false, false);
        assertEquals(STARTED, state.update(true, false, false));
        assertEquals(NONE, state.update(false, false, false));
        assertEquals(NONE, state.update(true, false, false));
        assertEquals(COMPLETED, state.update(true, true, false));
        assertEquals(NONE, state.complete());
        assertEquals(NONE, state.update(true, true, true));
        assertEquals(COMPLETED, new ProgressNoticeState(false, false, false).update(true, true, true));
    }
    @Test void reconnectAndResetHaveDifferentSemantics() {
        var state = new ProgressNoticeState(true, true, true);
        assertEquals(NONE, state.complete());
        assertEquals(NONE, state.update(true, true, true));
        assertEquals(NONE, state.update(false, false, false));
        assertEquals(STARTED, state.update(true, false, false));
        assertEquals(COMPLETED, state.complete());
        assertEquals(NONE, state.complete());
        assertEquals(NONE, new ProgressNoticeState(true, false, false).update(true, false, false));
    }
}

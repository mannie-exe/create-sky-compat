package design.inherent.createskycompat.questlog;

/** One quest lifecycle, independent of UI/network classes. Full synchronization is a silent baseline. */
public final class ProgressNoticeState {
    public enum Notice { NONE, STARTED, COMPLETED }
    private boolean started, completed, completionSent;

    public ProgressNoticeState(boolean progress, boolean complete, boolean sent) {
        started = progress || complete; completed = complete; completionSent = sent;
    }

    public Notice update(boolean progress, boolean complete, boolean sent) {
        if (completionSent && !sent && !complete) { started = false; completed = false; }
        completionSent = sent;
        if (complete && !completed) {
            completed = true; started = true; return Notice.COMPLETED;
        }
        if (progress && !started && !completed) { started = true; return Notice.STARTED; }
        return Notice.NONE;
    }
    public Notice complete() {
        completionSent = true;
        if (completed) return Notice.NONE;
        started = true; completed = true; return Notice.COMPLETED;
    }
}

package design.inherent.skyweavecompat.client;

/** Render-thread state: an opaque pass must precede the translucent pass in this pipeline/frame. */
public final class OpaqueDepthFrame {
    private Object owner;
    private int frame = -1;
    private boolean snapshot;
    public void opaque(Object pipeline, int current) { owner = pipeline; frame = current; snapshot = false; }
    public void translucent(Object pipeline, int current) { if (valid(pipeline, current)) snapshot = true; }
    public boolean valid(Object pipeline, int current) { return pipeline != null && owner == pipeline && frame == current; }
    public boolean snapshot() { return snapshot; }
    public void clear(Object pipeline) { if (owner == pipeline) { owner = null; frame = -1; snapshot = false; } }
}

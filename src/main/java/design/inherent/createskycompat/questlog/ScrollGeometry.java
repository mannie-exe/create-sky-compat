package design.inherent.createskycompat.questlog;

/** Shared track geometry for drawing, hit testing and dragging, in GUI pixels. */
public record ScrollGeometry(double maximum, double offset, double thumbStart, double thumbLength, double travel) {
    public static ScrollGeometry of(double viewport, double content, double offset) {
        viewport = Math.max(0, viewport);
        double maximum = Math.max(0, content - viewport);
        offset = Math.clamp(offset, 0, maximum);
        double length = content <= 0 ? viewport : Math.min(viewport, Math.max(12, viewport * viewport / content));
        double travel = viewport - length;
        return new ScrollGeometry(maximum, offset, maximum == 0 ? 0 : offset / maximum * travel, length, travel);
    }

    public double drag(double delta) {
        return travel <= 0 ? 0 : Math.clamp(offset + delta * maximum / travel, 0, maximum);
    }
}

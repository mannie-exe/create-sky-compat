package design.inherent.createskycompat.questlog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScrollGeometryTest {
    @Test void emptyAndShortContentCannotScroll() {
        for (int content : new int[]{0, 10, 100}) {
            var g = ScrollGeometry.of(100, content, 300);
            assertEquals(0, g.maximum()); assertEquals(0, g.drag(100));
            assertEquals(100, g.thumbLength());
        }
        assertEquals(0, ScrollGeometry.of(0, 100, 5).drag(20));
    }
    @Test void thumbAndDragShareTheSameTravel() {
        var g = ScrollGeometry.of(100, 400, 100);
        assertEquals(25, g.thumbLength()); assertEquals(25, g.thumbStart());
        assertEquals(100, g.drag(0)); assertEquals(300, g.drag(50));
        assertEquals(0, g.drag(-100));
        assertEquals(12, ScrollGeometry.of(100, 10000, 0).thumbLength());
        assertEquals(0, ScrollGeometry.of(100, 400, -5).offset());
    }
}

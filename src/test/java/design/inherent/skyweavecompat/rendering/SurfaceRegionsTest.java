package design.inherent.skyweavecompat.rendering;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import design.inherent.skyweavecompat.rendering.SurfaceRegions.Rect;

class SurfaceRegionsTest {
    private static double area(Rect r) { return (r.u1() - r.u0()) * (r.v1() - r.v0()); }

    @Test void partialAndOverlappingContactsCoverOnceWithoutRemovingHoles() {
        var source = new Rect(0, 0, 1, 1);
        var a = new Rect(0, .5f, 1, 1); var b = new Rect(.5f, 0, 1, 1);
        var parts = SurfaceRegions.partition(source, List.of(a, b, a));
        assertEquals(1, parts.stream().mapToDouble(p -> area(p.bounds())).sum(), 1e-7);
        assertEquals(.75, parts.stream().filter(SurfaceRegions.Part::shared).mapToDouble(p -> area(p.bounds())).sum(), 1e-7);
        for (int i = 0; i < parts.size(); i++) for (int j = i + 1; j < parts.size(); j++)
            assertNull(parts.get(i).bounds().intersection(parts.get(j).bounds()));
        assertEquals(parts, SurfaceRegions.partition(source, List.of(b, a)));
    }

    @Test void touchingEdgesHaveNoAreaAndFullCoverageMergesAcrossProcessorTiles() {
        var source = new Rect(0, 0, 1, 1);
        assertEquals(List.of(new SurfaceRegions.Part(source, false)),
                SurfaceRegions.partition(source, List.of(new Rect(1, 0, 2, 1))));
        assertEquals(List.of(new SurfaceRegions.Part(source, true)),
                SurfaceRegions.partition(source, List.of(new Rect(0, 0, .5f, 1), new Rect(.5f, 0, 1, 1))));
    }

    @Test void invalidRectanglesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Rect(0, 0, Float.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new Rect(1, 0, 0, 1));
    }

    @Test void complexUnknownCoverageRetainsUnseparatedBackfaceWithinTheWorkLimit() {
        var source = new Rect(0, 0, 1, 1);
        var contacts = new java.util.ArrayList<Rect>();
        for (int i = 1; i < 70; i++) contacts.add(new Rect(i / 100f, i / 100f, (i + .25f) / 100, (i + .25f) / 100));
        assertEquals(List.of(new SurfaceRegions.Part(source, false)), SurfaceRegions.partition(source, contacts));
    }
}

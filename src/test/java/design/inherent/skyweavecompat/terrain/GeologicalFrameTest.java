package design.inherent.skyweavecompat.terrain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeologicalFrameTest {
    @Test void nativeBodyMappingUsesTheNominalHeightWithoutChangingTheSurfaceDatum() {
        for (int height = 250; height <= 400; height++) {
            var profile = GenerationProfile.INITIAL;
            var frame = GeologicalFrame.nativeBody(400, 28, profile.cliffDepth(height), 48, false);
            assertEquals(428, frame.surface());
            assertEquals(height, frame.topExclusive() - frame.bottom(), 1);
            assertEquals(frame.bottom(), frame.toWorld(-64));
            assertEquals(428, frame.toWorld(64));
            assertEquals(493, frame.toWorld(129));
        }
        var wet = GeologicalFrame.nativeBody(400, 28, 300, 48, true);
        assertEquals(376, wet.topExclusive() - wet.bottom());
    }

    @Test void distinguishesUndergroundSurfaceAndBuildCeiling() {
        var frame = new GeologicalFrame(180, 509);
        assertEquals(180, frame.toWorld(-64));
        assertEquals(508, frame.toWorld(64));
        assertEquals(344, frame.toWorld(0));
        assertEquals(763, frame.toWorld(319));
        assertEquals(64, frame.toReference(508));
        assertTrue(frame.containsReference(-64));
        assertTrue(frame.containsReference(319));
        assertFalse(frame.containsReference(-65));
        assertFalse(frame.containsReference(320));
        // Out-of-range coordinates stay outside; placement rejects them instead of clamping.
        assertTrue(frame.toWorld(-65) < frame.bottom());
        assertEquals(frame.surface() + 256, frame.toWorld(320));
    }

    @Test void mappingIsMonotonicAndInverseHasOnlyQuantizationError() {
        for (var frame : new GeologicalFrame[]{new GeologicalFrame(180, 509), new GeologicalFrame(-100, 450)}) {
            int previous = Integer.MIN_VALUE;
            for (int y = -80; y <= 400; y++) {
                int world = frame.toWorld(y);
                assertTrue(world >= previous);
                assertTrue(Math.abs(frame.toReference(world) - y) <= 2);
                previous = world;
            }
        }
    }

    @Test void rejectsEmptyAndInvertedFrames() {
        assertThrows(IllegalArgumentException.class, () -> new GeologicalFrame(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new GeologicalFrame(3, 2));
    }
}

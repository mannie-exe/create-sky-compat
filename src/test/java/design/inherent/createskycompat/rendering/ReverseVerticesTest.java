package design.inherent.createskycompat.rendering;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReverseVerticesTest {
    @Test void reversesWindingAndNormalsWithoutSeparatingAttributes() {
        int[] source = new int[32];
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int attribute = 0; attribute < 7; attribute++) source[vertex * 8 + attribute] = vertex * 100 + attribute;
            source[vertex * 8 + 7] = 0x42007f81;
        }
        int[] before = source.clone();
        int[] reversed = ReverseVertices.copy(source);
        assertArrayEquals(before, source);
        assertNotSame(source, reversed);
        for (int vertex = 0; vertex < 4; vertex++) {
            int original = (4 - vertex) % 4;
            for (int attribute = 0; attribute < 7; attribute++)
                assertEquals(source[original * 8 + attribute], reversed[vertex * 8 + attribute]);
            assertEquals(0x4200817f, reversed[vertex * 8 + 7]);
        }
        assertArrayEquals(source, ReverseVertices.copy(reversed));
    }

    @Test void rejectsUnknownVertexFormats() {
        assertThrows(IllegalArgumentException.class, () -> ReverseVertices.copy(new int[28]));
    }
}

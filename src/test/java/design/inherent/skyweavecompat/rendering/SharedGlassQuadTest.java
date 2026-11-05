package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.client.GlassBackfaces;
import design.inherent.skyweavecompat.client.GlassSurfaceContacts;
import design.inherent.skyweavecompat.rendering.SurfaceRegions.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedGlassQuadTest {
    private static BakedQuad quad(Direction direction) {
        int[] data = new int[32];
        int axis = direction.getAxis().ordinal(), u = (axis + 1) % 3, v = (axis + 2) % 3;
        int step = direction.getAxisDirection().getStep();
        float[][] points = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int i = 0; i < 4; i++) {
            int corner = step > 0 ? i : (4 - i) % 4;
            float a = points[corner][0], b = points[corner][1];
            data[i * 8 + axis] = Float.floatToIntBits(step > 0 ? 1 : 0);
            data[i * 8 + u] = Float.floatToIntBits(a); data[i * 8 + v] = Float.floatToIntBits(b);
            data[i * 8 + 3] = 0xffb08040;
            data[i * 8 + 4] = Float.floatToIntBits(2 + 8 * a);
            data[i * 8 + 5] = Float.floatToIntBits(3 + 6 * b);
            data[i * 8 + 6] = 0x00f00070;
            data[i * 8 + 7] = (step * 127 & 255) << axis * 8;
        }
        return new BakedQuad(data, 4, direction, null, false, false);
    }

    private static GlassSurfaceContacts contacts(Direction side, Rect rect) {
        var sides = new ArrayList<List<Rect>>();
        for (Direction direction : Direction.values()) sides.add(direction == side ? List.of(rect) : List.of());
        return new GlassSurfaceContacts(sides);
    }

    @Test void contactSkinMovesInwardForEveryDirectionWithoutMutatingOriginalOrUv() {
        for (Direction direction : Direction.values()) {
            var original = quad(direction); int[] saved = original.getVertices().clone();
            var context = contacts(direction, new Rect(0, 0, 1, 1));
            var output = GlassBackfaces.append(List.of(original), context);
            assertEquals(2, output.size()); assertSame(original, output.getFirst());
            var reverse = output.getLast(); int axis = direction.getAxis().ordinal();
            assertEquals(direction.getOpposite(), reverse.getDirection());
            for (int i = 0; i < 4; i++) {
                float position = Float.intBitsToFloat(reverse.getVertices()[i * 8 + axis]);
                assertTrue(position > 0 && position < 1);
                int source = (4 - i) % 4;
                for (int attribute = 3; attribute <= 6; attribute++)
                    assertEquals(saved[source * 8 + attribute], reverse.getVertices()[i * 8 + attribute]);
            }
            assertArrayEquals(saved, original.getVertices());
            assertSame(output, GlassBackfaces.append(output, context));
        }
    }

    @Test void partialContactSplitsOnlyCoveredAreaAndKeepsAffineTextureCoordinates() {
        var original = quad(Direction.SOUTH);
        var output = GlassBackfaces.append(List.of(original), contacts(Direction.SOUTH, new Rect(.4f, .25f, .6f, .75f)));
        double covered = 0, total = 0;
        for (var reverse : output.subList(1, output.size())) {
            float x0 = 1, y0 = 1, x1 = 0, y1 = 0;
            for (int i = 0; i < 4; i++) {
                int[] data = reverse.getVertices();
                float x = Float.intBitsToFloat(data[i * 8]), y = Float.intBitsToFloat(data[i * 8 + 1]);
                x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y);
                assertEquals(2 + 8 * x, Float.intBitsToFloat(data[i * 8 + 4]), 1e-6);
                assertEquals(3 + 6 * y, Float.intBitsToFloat(data[i * 8 + 5]), 1e-6);
                assertEquals(0xffb08040, data[i * 8 + 3]); assertEquals(0x00f00070, data[i * 8 + 6]);
                assertEquals(4, reverse.getTintIndex()); assertFalse(reverse.isShade()); assertFalse(reverse.hasAmbientOcclusion());
            }
            double area = (x1 - x0) * (y1 - y0); total += area;
            if (Float.intBitsToFloat(reverse.getVertices()[2]) < 1) covered += area;
        }
        assertEquals(1, total, 1e-6); assertEquals(.1, covered, 1e-6);
        assertSame(output, GlassBackfaces.append(output));
    }

    @Test void nonoverlappingNeighborLeavesReverseVerticesExact() {
        var original = quad(Direction.SOUTH);
        var output = GlassBackfaces.append(List.of(original), contacts(Direction.SOUTH, new Rect(2, 0, 3, 1)));
        assertArrayEquals(ReverseVertices.copy(original.getVertices()), output.getLast().getVertices());
    }

    @Test void nestedSamplingSkipsRecaptureAndExceptionsRestoreTheThread() {
        var view = (net.minecraft.world.level.BlockAndTintGetter) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{net.minecraft.world.level.BlockAndTintGetter.class},
                (proxy, method, args) -> {
                    assertTrue(GlassSurfaceContacts.sampling());
                    assertNull(GlassSurfaceContacts.capture((net.minecraft.world.level.BlockAndTintGetter) proxy,
                            net.minecraft.core.BlockPos.ZERO));
                    throw new IllegalStateException("Fixture neighbor failure");
                });
        assertThrows(IllegalStateException.class,
                () -> GlassSurfaceContacts.capture(view, net.minecraft.core.BlockPos.ZERO));
        assertFalse(GlassSurfaceContacts.sampling());
    }
}

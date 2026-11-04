package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.client.GlassBackfaces;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlassBackfacesTest {
    @Test void preservesMetadataAndDoesNotGrowRepeatedOutput() {
        int[] vertices = new int[32];
        for (int i = 0; i < 4; i++) {
            vertices[i * 8] = Float.floatToIntBits(i % 2);
            vertices[i * 8 + 1] = Float.floatToIntBits(i / 2);
        }
        var source = new BakedQuad(vertices, 3, Direction.SOUTH, null, false, false);
        var input = List.of(source);
        var output = GlassBackfaces.append(input);
        assertEquals(1, input.size());
        assertEquals(2, output.size());
        var reversed = output.get(1);
        assertSame(source, output.getFirst());
        assertNotSame(vertices, reversed.getVertices());
        assertEquals(Direction.NORTH, reversed.getDirection());
        assertEquals(3, reversed.getTintIndex());
        assertFalse(reversed.isShade());
        assertFalse(reversed.hasAmbientOcclusion());
        assertSame(output, GlassBackfaces.append(output));
    }

    @Test void leavesExistingCoplanarReverseAndEmptyOutputAlone() {
        var a = new BakedQuad(new int[32], -1, Direction.UP, null, true);
        var b = new BakedQuad(new int[32], -1, Direction.DOWN, null, true);
        var input = List.of(a, b);
        assertSame(input, GlassBackfaces.append(input));
        var empty = List.<BakedQuad>of();
        assertSame(empty, GlassBackfaces.append(empty));
    }
}

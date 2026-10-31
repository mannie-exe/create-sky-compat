package design.inherent.createskycompat.rendering;

import design.inherent.createskycompat.client.BlockMaterialScope;
import design.inherent.createskycompat.client.ColorwheelMaterials;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorwheelMaterialsTest {
    private static final BlockMaterialScope.Material OUTER = new BlockMaterialScope.Material(10001, (byte) 15, 3, 4, 5);
    private static final BlockMaterialScope.Material INNER = new BlockMaterialScope.Material(10002, (byte) 0, 6, 7, 8);

    @Test void directConstructDrawUsesColorwheelMetadataAndBalancesRecursiveFailure() {
        var buffer = new ColorwheelBuffer();
        BlockMaterialScope.renderWith(buffer, OUTER, ColorwheelMaterials.WRITER, () -> {
            assertEquals(OUTER, buffer.current);
            assertTrue(buffer.terrain);
            BlockMaterialScope.renderWith(buffer, OUTER, ColorwheelMaterials.WRITER, () -> assertEquals(1, buffer.begins));
            assertThrows(IllegalStateException.class, () ->
                    BlockMaterialScope.renderWith(buffer, INNER, ColorwheelMaterials.WRITER, () -> {
                        assertEquals(INNER, buffer.current);
                        throw new IllegalStateException("model failed");
                    }));
            assertEquals(OUTER, buffer.current);
        });
        assertNull(buffer.current);
        assertEquals(3, buffer.begins);
        assertEquals(1, buffer.ends);
    }

    @Test void plainIrisEmitterKeepsItsOwnProtocol() {
        var buffer = new IrisBuffer();
        BlockMaterialScope.renderWith(buffer, OUTER, ColorwheelMaterials.WRITER,
                () -> assertEquals(OUTER, buffer.current));
        assertNull(buffer.current);
    }

    @Test void nestedWritersRestoreTheParentsProtocol() {
        var buffer = new ColorwheelBuffer();
        BlockMaterialScope.render(buffer, OUTER, () -> {
            assertEquals(0, buffer.begins);
            BlockMaterialScope.renderWith(buffer, INNER, ColorwheelMaterials.WRITER,
                    () -> assertEquals(INNER, buffer.current));
            assertEquals(OUTER, buffer.current);
            assertEquals(1, buffer.begins);
        });
        assertNull(buffer.current);
    }

    private static class IrisBuffer implements BlockSensitiveBufferBuilder {
        BlockMaterialScope.Material current;
        public void beginBlock(int id, byte type, byte emission, int x, int y, int z) {
            assertEquals(0, type);
            current = new BlockMaterialScope.Material(id, emission, x, y, z);
        }
        public void endBlock() { current = null; }
        public void overrideBlock(int id) { throw new AssertionError(); }
        public void restoreBlock() { throw new AssertionError(); }
        public void ignoreMidBlock(boolean value) { throw new AssertionError(); }
    }
    private static final class ColorwheelBuffer extends IrisBuffer implements ColorwheelBufferBuilder {
        int begins, ends;
        boolean terrain;
        public void clrwlBeginBlock(int id, byte type, byte emission, boolean isTerrain, int x, int y, int z) {
            begins++; terrain = isTerrain;
            beginBlock(id, type, emission, x, y, z);
        }
        public void clrwlEndBlock() { ends++; endBlock(); }
    }
}

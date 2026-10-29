package design.inherent.createskycompat.rendering;

import design.inherent.createskycompat.client.BlockMaterialScope;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BlockMaterialScopeTest {
    private static final BlockMaterialScope.Material OUTER = new BlockMaterialScope.Material(10001, (byte) 7, 3, 4, 5);
    private static final BlockMaterialScope.Material INNER = new BlockMaterialScope.Material(10002, (byte) 0, 6, 7, 8);

    @Test void appliesMaterialOnlyForTheDrawAndClearsOnFailure() {
        var buffer = new Buffer();
        assertThrows(IllegalStateException.class, () -> BlockMaterialScope.render(buffer, OUTER, () -> {
            assertEquals(OUTER, buffer.current);
            throw new IllegalStateException("model failed");
        }));
        assertNull(buffer.current);
        assertEquals(List.of("begin:10001", "end"), buffer.operations);
        BlockMaterialScope.render(buffer, INNER, () -> assertEquals(INNER, buffer.current));
        assertNull(buffer.current);
    }

    @Test void recursiveDrawRestoresOuterMaterialWithoutDuplicateSameStateAnnotations() {
        var buffer = new Buffer();
        BlockMaterialScope.render(buffer, OUTER, () -> {
            BlockMaterialScope.render(buffer, OUTER, () -> assertEquals(OUTER, buffer.current));
            BlockMaterialScope.render(buffer, INNER, () -> assertEquals(INNER, buffer.current));
            assertEquals(OUTER, buffer.current);
        });
        assertEquals(List.of("begin:10001", "begin:10002", "begin:10001", "end"), buffer.operations);
    }

    @Test void differentBuffersHaveIndependentScopes() {
        var first = new Buffer();
        var second = new Buffer();
        BlockMaterialScope.render(first, OUTER, () -> {
            BlockMaterialScope.render(second, INNER, () -> {
                assertEquals(OUTER, first.current);
                assertEquals(INNER, second.current);
            });
            assertEquals(OUTER, first.current);
            assertNull(second.current);
        });
        assertNull(first.current);
    }

    @Test void failedAnnotationDoesNotLeaveThreadStateBehind() {
        var buffer = new Buffer();
        buffer.failBegin = true;
        assertThrows(IllegalStateException.class, () -> BlockMaterialScope.render(buffer, OUTER, () -> fail("draw must not run")));
        buffer.failBegin = false;
        BlockMaterialScope.render(buffer, OUTER, () -> assertEquals(OUTER, buffer.current));
        assertNull(buffer.current);
        assertEquals(List.of("begin:10001", "end", "begin:10001", "end"), buffer.operations);
    }

    private static final class Buffer implements BlockSensitiveBufferBuilder {
        BlockMaterialScope.Material current;
        boolean failBegin;
        final List<String> operations = new ArrayList<>();
        public void beginBlock(int id, byte type, byte emission, int x, int y, int z) {
            assertEquals(0, type);
            operations.add("begin:" + id);
            if (failBegin) throw new IllegalStateException("buffer failed");
            current = new BlockMaterialScope.Material(id, emission, x, y, z);
        }
        public void endBlock() { operations.add("end"); current = null; }
        public void overrideBlock(int id) { throw new AssertionError(); }
        public void restoreBlock() { throw new AssertionError(); }
        public void ignoreMidBlock(boolean value) { throw new AssertionError(); }
    }
}

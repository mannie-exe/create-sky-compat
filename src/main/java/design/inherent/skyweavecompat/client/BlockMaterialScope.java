package design.inherent.skyweavecompat.client;

import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import java.util.IdentityHashMap;

/** Balances Iris annotations across exceptions and recursive model rendering. */
public final class BlockMaterialScope {
    private static final ThreadLocal<IdentityHashMap<BlockSensitiveBufferBuilder, Frame>> ACTIVE =
            ThreadLocal.withInitial(IdentityHashMap::new);

    public interface Writer {
        void begin(BlockSensitiveBufferBuilder buffer, Material material);
        void end(BlockSensitiveBufferBuilder buffer);
    }

    private static final Writer IRIS = new Writer() {
        public void begin(BlockSensitiveBufferBuilder buffer, Material material) { material.begin(buffer); }
        public void end(BlockSensitiveBufferBuilder buffer) { buffer.endBlock(); }
    };
    private record Frame(Material material, Writer writer) {
        void begin(BlockSensitiveBufferBuilder buffer) { writer.begin(buffer, material); }
    }

    private BlockMaterialScope() {}

    public record Material(int id, byte emission, int x, int y, int z) {
        void begin(BlockSensitiveBufferBuilder buffer) { buffer.beginBlock(id, (byte) 0, emission, x, y, z); }
    }

    public static void render(BlockSensitiveBufferBuilder buffer, Material material, Runnable render) {
        renderWith(buffer, material, IRIS, render);
    }

    public static void renderWith(BlockSensitiveBufferBuilder buffer, Material material, Writer writer, Runnable render) {
        var active = ACTIVE.get();
        var previous = active.get(buffer);
        var current = new Frame(material, writer);
        if (current.equals(previous)) { render.run(); return; }
        active.put(buffer, current);
        try {
            current.begin(buffer);
            render.run();
        } finally {
            try {
                if (previous == null) writer.end(buffer);
                else previous.begin(buffer);
            } finally {
                if (previous == null) active.remove(buffer);
                else active.put(buffer, previous);
                if (active.isEmpty()) ACTIVE.remove();
            }
        }
    }
}

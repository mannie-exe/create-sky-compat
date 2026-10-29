package design.inherent.createskycompat.client;

import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import java.util.IdentityHashMap;

/** Balances Iris annotations across exceptions and recursive model rendering. */
public final class BlockMaterialScope {
    private static final ThreadLocal<IdentityHashMap<BlockSensitiveBufferBuilder, Material>> ACTIVE =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private BlockMaterialScope() {}

    public record Material(int id, byte emission, int x, int y, int z) {
        void begin(BlockSensitiveBufferBuilder buffer) { buffer.beginBlock(id, (byte) 0, emission, x, y, z); }
    }

    public static void render(BlockSensitiveBufferBuilder buffer, Material material, Runnable render) {
        var active = ACTIVE.get();
        var previous = active.get(buffer);
        if (material.equals(previous)) { render.run(); return; }
        active.put(buffer, material);
        try {
            material.begin(buffer);
            render.run();
        } finally {
            try {
                if (previous == null) buffer.endBlock();
                else previous.begin(buffer);
            } finally {
                if (previous == null) active.remove(buffer);
                else active.put(buffer, previous);
                if (active.isEmpty()) ACTIVE.remove();
            }
        }
    }
}

package design.inherent.createskycompat.terrain;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

/** Original provider semantics, independent of the target dimension's expanded height. */
public final class ReferenceHeightContext extends WorldGenerationContext {
    public ReferenceHeightContext(ChunkGenerator generator, LevelHeightAccessor level) { super(generator, level); }
    @Override public int getMinGenY() { return GeologicalFrame.REFERENCE_BOTTOM; }
    @Override public int getGenDepth() { return GeologicalFrame.REFERENCE_HEIGHT; }
}

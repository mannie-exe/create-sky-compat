package design.inherent.skyweavecompat.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

/** Per-call context for native surface helpers; restored even when painting fails. */
public final class IslandSurfaceScope {
    private record Context(IslandField field, SkyIslandSettings settings) {}
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private IslandSurfaceScope() {}

    public static void run(IslandField field, SkyIslandSettings settings, Runnable action) {
        var previous = CURRENT.get();
        if (((ProfiledTerrain)(Object)field).skyweave$profile() == null) CURRENT.remove();
        else CURRENT.set(new Context(field, settings));
        try { action.run(); }
        finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    /** Null means the native write would refill an authored opening or fluid. */
    public static BlockState surface(ChunkAccess chunk, BlockPos pos, BlockState selected) {
        var context = CURRENT.get();
        if (context == null) return selected;
        var current = chunk.getBlockState(pos);
        if (current.isAir() || !current.getFluidState().isEmpty()) return null;
        int top = context.field.sampleColumnProfile(pos.getX(), pos.getZ(), context.settings).topY();
        var biome = chunk.getNoiseBiome(pos.getX() >> 2, top >> 2, pos.getZ() >> 2);
        return BiomeMaterials.surface(selected, biome, top - pos.getY());
    }

    public static int referenceY(int x, int y, int z) {
        var context = CURRENT.get();
        if (context == null) return y;
        var frame = ((GeologicalField)(Object)context.field).skyweave$frameAt(x, z, context.settings);
        return frame == null ? y : frame.toReference(y);
    }
}

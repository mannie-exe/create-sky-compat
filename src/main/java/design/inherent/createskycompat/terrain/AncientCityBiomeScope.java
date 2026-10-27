package design.inherent.createskycompat.terrain;

import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector;

/** Confine relocated-city eligibility to the same column band as stored Deep Dark. */
public final class AncientCityBiomeScope {
    private record Context(SkyIslandChunkGenerator generator, RandomState random) {}
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private AncientCityBiomeScope() {}

    public static SkyIslandDeepDarkBiomeProjector.Scope enter(SkyIslandChunkGenerator generator,
            RandomState random, SkyIslandDeepDarkBiomeProjector.Scope upstream) {
        var previous = ACTIVE.get();
        ACTIVE.set(new Context(generator, random));
        return () -> {
            try { upstream.close(); }
            finally {
                if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
            }
        };
    }

    public static boolean active() { return ACTIVE.get() != null; }

    public static int queryQuartY(int qx, int originalY, int qz) {
        var context = ACTIVE.get();
        if (context == null) return originalY;
        var generator = context.generator();
        var field = ((GeologicalGenerator)(Object)generator).createSky$field(context.random());
        var exterior = ((InteriorField)(Object)field).createSky$exteriorAt(qx * 4 + 2, qz * 4 + 2,
                generator.resolvedSkyIslandSettings());
        if (exterior.isEmpty()) return originalY;
        var column = exterior.getFirst();
        // Both upstream's midpoint precheck and the unshifted Jigsaw stub can lie
        // outside its lower-45% projection. Use a point inside that band for both.
        return (column.bottomY() + column.thickness() / 4) >> 2;
    }
}

package design.inherent.skyweavecompat.terrain;

import org.junit.jupiter.api.Test;
import org.sathrek.sky_archipelago.worldgen.generator.field.*;
import org.sathrek.sky_archipelago.worldgen.generator.field.model.*;
import static org.junit.jupiter.api.Assertions.*;

class OceanIslandsTest {
    private static final long SEED = 17;
    private IslandDescriptor island(IslandShapeArchetype archetype) {
        return new IslandDescriptor(IslandFamily.ANCHOR_PLATEAU, 0, 0, 0, 64, 64, 64, 0,
                archetype, 28, 100, 48, 0, 0, 24, 24, .4, SEED);
    }
    private WaterArchetypeColumnProfile shore() {
        return new WaterArchetypeColumnProfile(WaterArchetypeColumnProfile.Region.SHORE,
                WaterArchetypeColumnProfile.SurfaceRole.SANDY,
                WaterArchetypeColumnProfile.BiomeProjection.BEACH_SHORE, 40, 34, 36, 0, true);
    }
    private HorizontalSample point(double x, double z) { return new HorizontalSample(x, z, .5, .5, 1, true); }

    @Test void coastalOpeningIsDirectionalAndPreservesNativeFluidAndRockProvenance() {
        var profile = new OceanProfile(true, 1, 0, 40, 64, 4, 2, 4);
        double angle = OceanProfile.unit(SEED ^ 0x179d5b396a1cb874L) * Math.PI * 2;
        var source = shore();
        var changed = OceanIslands.adapt(island(IslandShapeArchetype.LAGOON),
                point(50 * Math.cos(angle), 50 * Math.sin(angle)), source, profile);
        assertTrue(changed.water());
        assertEquals(34, changed.fluidSurfaceY());
        assertTrue(changed.configuredOceanFluid());
        assertEquals(0, changed.underbellyBottomY());
        assertTrue(changed.containsProtectedSealAt(changed.solidSurfaceY()));
        assertSame(source, OceanIslands.adapt(island(IslandShapeArchetype.LAGOON),
                point(-50 * Math.cos(angle), -50 * Math.sin(angle)), source, profile));
    }

    @Test void perimeterReusesNativeFootprintWhileDisabledAndDryArchetypesStayUnchanged() {
        var profile = new OceanProfile(true, 0, 1, 40, 64, 2, 2, 4);
        var source = shore();
        for (var type : new IslandShapeArchetype[]{IslandShapeArchetype.LAGOON, IslandShapeArchetype.REEF_RING}) {
            assertTrue(OceanIslands.adapt(island(type), point(-60, 20), source, profile).water());
            assertSame(source, OceanIslands.adapt(island(type), point(0, 0), source, OceanProfile.INITIAL));
            assertSame(WaterArchetypeColumnProfile.OUTSIDE, OceanIslands.adapt(island(type), point(1000, 0),
                    WaterArchetypeColumnProfile.OUTSIDE, profile));
        }
        assertSame(source, OceanIslands.adapt(island(IslandShapeArchetype.DEAD_OCEAN_SHELF), point(0, 0), source, profile));
    }
}

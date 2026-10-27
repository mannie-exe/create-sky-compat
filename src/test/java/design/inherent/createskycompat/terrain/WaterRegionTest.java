package design.inherent.createskycompat.terrain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaterRegionTest {
    @Test void enclosedWaterHasProtectedRockOnEverySideExceptItsSurface() {
        for (int radius : new int[] {8, 20, 36}) for (int depth : new int[] {3, 12, 32}) {
            var region = new WaterRegion(1, 0, 0, 200, radius, depth, WaterRegion.Kind.ENCLOSED, 0, 100, 1, 2, 2);
            int[][] offsets = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, -1, 0}};
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                for (int y = region.level() - depth; y <= region.level(); y++) {
                    if (!region.source(x, y, z)) continue;
                    for (var offset : offsets) {
                        int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
                        assertTrue(region.source(nx, ny, nz) || region.protects(nx, ny, nz),
                                "Unprotected water boundary at " + nx + "," + ny + "," + nz + "; radius=" + radius + ", depth=" + depth);
                    }
                }
        }
    }

    @Test void undersideOutletCutsRockWithoutFillingItsShaftWithSourceWater() {
        var region = new WaterRegion(1, 10, -10, 300, 24, 9, WaterRegion.Kind.OUTLET, 0, 100, 1, 2, 3);
        int x = 20, z = -10;
        assertTrue(region.shaft(x, z));
        assertTrue(region.source(x, 300, z));
        assertTrue(region.cuts(x, 0, z));
        assertFalse(region.source(x, 0, z));
        assertFalse(region.protects(x, region.bed(x, z), z));
    }
}

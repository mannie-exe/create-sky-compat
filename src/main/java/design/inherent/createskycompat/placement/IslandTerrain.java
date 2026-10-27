package design.inherent.createskycompat.placement;

import org.sathrek.sky_archipelago.worldgen.structure.StructureSupportContext;

public record IslandTerrain(StructureSupportContext context) implements NeighborRule.Terrain {
    @Override
    public boolean exists(int x, int z) {
        // Village clearance reserves the exterior footprint, including cavern entrances.
        return context.islandField().sampleColumnProfile(x, z, context.settings()).exists();
    }

    @Override
    public int islandRadius(int x, int z) {
        var descriptor = context.islandField().sampleDominantIslandDescriptor(x, z, context.settings());
        return descriptor == null ? 0 : descriptor.maxRadius();
    }
}

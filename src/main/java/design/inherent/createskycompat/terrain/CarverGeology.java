package design.inherent.createskycompat.terrain;

import net.minecraft.world.level.LevelHeightAccessor;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;

/** One carving context's immutable geography binding; never changes world bounds or tunnel geometry. */
public record CarverGeology(SkyIslandChunkGenerator generator, IslandField field, ReferenceHeightContext reference) {
    public CarverGeology(SkyIslandChunkGenerator generator, IslandField field) {
        this(generator, field, new ReferenceHeightContext(generator,
                LevelHeightAccessor.create(GeologicalFrame.REFERENCE_BOTTOM, GeologicalFrame.REFERENCE_HEIGHT)));
    }

    public int worldHeight(int sampled, int x, int z, int rejected) {
        var frame = ((GeologicalField)(Object)field).createSky$frameAt(x, z, generator.resolvedSkyIslandSettings());
        return frame == null || !frame.containsReference(sampled) ? rejected : frame.toWorld(sampled);
    }
}

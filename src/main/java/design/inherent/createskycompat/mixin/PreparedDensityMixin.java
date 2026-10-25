package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator.PreparedDensity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PreparedDensity.class, remap = false)
public abstract class PreparedDensityMixin implements PreparedIslandShape {
    @Unique private IslandUnderside.Bounds createSky$bounds;
    @Override public IslandUnderside.Bounds createSky$bounds() { return createSky$bounds; }
    @Override public void createSky$bounds(IslandUnderside.Bounds bounds) { createSky$bounds = bounds; }
}

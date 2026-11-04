package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.*;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator.PreparedDensity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PreparedDensity.class, remap = false)
public abstract class PreparedDensityMixin implements PreparedIslandShape {
    @Unique private IslandUnderside.Bounds skyweave$bounds;
    @Override public IslandUnderside.Bounds skyweave$bounds() { return skyweave$bounds; }
    @Override public void skyweave$bounds(IslandUnderside.Bounds bounds) { skyweave$bounds = bounds; }
}

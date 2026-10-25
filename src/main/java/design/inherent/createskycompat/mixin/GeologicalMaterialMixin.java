package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.GeologicalFrame;
import design.inherent.createskycompat.terrain.GeologicalMaterial;
import org.sathrek.sky_archipelago.worldgen.generator.terrain.SkyIslandColumnMaterialPlan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = SkyIslandColumnMaterialPlan.class, remap = false)
public abstract class GeologicalMaterialMixin implements GeologicalMaterial {
    @Unique private GeologicalFrame createSky$frame;
    @Override public void createSky$setFrame(GeologicalFrame frame) { createSky$frame = frame; }

    @ModifyVariable(method = "usesDeepslateAtY", at = @At("HEAD"), argsOnly = true)
    private int geologicalY(int y) {
        return createSky$frame == null ? y : createSky$frame.toReference(y);
    }
}

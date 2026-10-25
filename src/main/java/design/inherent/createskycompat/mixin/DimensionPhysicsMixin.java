package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.climate.IslandClimate;
import design.inherent.createskycompat.terrain.GenerationProfile;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysics;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

/** Routes a namespaced, natively synchronized physics profile only into its matching dimension type. */
@Mixin(value = DimensionPhysicsData.class, remap = false)
public abstract class DimensionPhysicsMixin {
    @Shadow @Final private static Map<ResourceKey<Level>, DimensionPhysics> DIMENSION_PHYSICS_DATA;
    @Unique private static final ResourceKey<Level> CREATE_SKY_PHYSICS = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.parse(GenerationProfile.ID));

    @Inject(method = "of", at = @At("RETURN"), cancellable = true)
    private static void islandPhysics(Level level, CallbackInfoReturnable<DimensionPhysics> result) {
        if (!IslandClimate.applies(level)) return;
        var profile = DIMENSION_PHYSICS_DATA.get(CREATE_SKY_PHYSICS);
        // A higher-priority explicit world override remains authoritative. Missing data retains Sable's fallback.
        if (profile != null && profile.priority() >= result.getReturnValue().priority()) result.setReturnValue(profile);
    }
}

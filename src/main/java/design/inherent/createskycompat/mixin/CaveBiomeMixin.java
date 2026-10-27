package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extend the existing biome-fill scope without changing surface biomes or direct structure queries. */
@Mixin(value = SkyIslandDeepDarkBiomeProjector.class, remap = false)
public abstract class CaveBiomeMixin {
    @Shadow @Final private static ThreadLocal<?> ACTIVE;
    @Shadow @Final private static ThreadLocal<Boolean> SAMPLING_PROXY;

    @Inject(method = "project(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/core/Holder;IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"), cancellable = true)
    private static void relativeCaveBiome(BiomeResolver resolver, Holder<Biome> original, int qx, int qy, int qz,
            Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> result) {
        if (resolver == null || sampler == null || result.getReturnValue() == null || ACTIVE.get() == null
                || SAMPLING_PROXY.get()) return;
        var context = (DeepDarkContextAccessor)ACTIVE.get();
        var generator = context.createSky$generator();
        if (!((GeologicalGenerator)(Object)generator).createSky$isEnabled()) return;
        int x = qx * 4 + 2, y = qy * 4 + 2, z = qz * 4 + 2;
        // Both geometry and proxy-biome reads may reenter the upstream biome-source hook.
        SAMPLING_PROXY.set(true);
        try {
            var settings = generator.resolvedSkyIslandSettings();
            var field = ((GeologicalGenerator)(Object)generator).createSky$field(context.createSky$random());
            var ocean = OceanBiomes.project(generator.getBiomeSource(), original, field, settings, x, y, z);
            if (ocean != original) { result.setReturnValue(ocean); return; }
            if (result.getReturnValue().is(Biomes.DEEP_DARK)) return;
            var exteriors = ((InteriorField)(Object)field).createSky$exteriorAt(x, z, settings);
            if (exteriors.stream().noneMatch(column -> y >= column.bottomY() && y <= column.topY() - 16)) return;
            var frame = ((GeologicalField)(Object)field).createSky$frameAt(x, z, settings);
            if (frame == null || !frame.containsReference(frame.toReference(y))) return;
            var proxy = resolver.getNoiseBiome(qx, frame.toReference(y) >> 2, qz, sampler);
            if (proxy.is(Biomes.LUSH_CAVES) || proxy.is(Biomes.DRIPSTONE_CAVES)) result.setReturnValue(proxy);
        } finally {
            SAMPLING_PROXY.set(false);
        }
    }
}

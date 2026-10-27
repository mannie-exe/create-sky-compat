package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import design.inherent.createskycompat.terrain.AncientCityBiomeScope;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandDeepDarkBiomeProjector;
import org.spongepowered.asm.mixin.*;

/** Reuse upstream projection; stored biomes, other structures and city geometry stay native. */
@Mixin(value = SkyIslandDeepDarkBiomeProjector.class, remap = false)
public abstract class AncientCityProjectionMixin {
    @Shadow @Final private static ThreadLocal<Boolean> SAMPLING_PROXY;

    @WrapMethod(method = "project(Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/core/Holder;IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
    private static Holder<Biome> cityQuery(BiomeSource source, Holder<Biome> originalBiome,
            int qx, int qy, int qz, Climate.Sampler sampler, Operation<Holder<Biome>> original) {
        if (source == null || sampler == null || originalBiome == null
                || SAMPLING_PROXY.get() || !AncientCityBiomeScope.active())
            return original.call(source, originalBiome, qx, qy, qz, sampler);
        int sampleY;
        Holder<Biome> base;
        SAMPLING_PROXY.set(true);
        try {
            sampleY = AncientCityBiomeScope.queryQuartY(qx, qy, qz);
            base = source.getNoiseBiome(qx, sampleY, qz, sampler);
        } finally { SAMPLING_PROXY.set(false); }
        return original.call(source, base, qx, sampleY, qz, sampler);
    }
}

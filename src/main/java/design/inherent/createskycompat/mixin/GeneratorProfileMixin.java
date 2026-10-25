package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import org.spongepowered.asm.mixin.Shadow;
import design.inherent.createskycompat.terrain.ProfiledTerrain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SkyIslandChunkGenerator.class, remap = false)
public abstract class GeneratorProfileMixin implements GeologicalGenerator {
    @Shadow private IslandField getIslandField(RandomState random) { throw new AssertionError(); }
    @Shadow private volatile SkyIslandSettings resolvedSettingsCache;
    @Override public IslandField createSky$field(RandomState random) { return getIslandField(random); }
    @Override public boolean createSky$isEnabled() {
        return ((SkyIslandChunkGenerator)(Object)this).generatorSettings().is(ResourceLocation.parse(GenerationProfile.ID));
    }

    @Inject(method = "resolvedSkyIslandSettings", at = @At("HEAD"), cancellable = true)
    private void savedPresetSettings(CallbackInfoReturnable<SkyIslandSettings> result) {
        if (!createSky$isEnabled()) return;
        var cached = resolvedSettingsCache;
        if (cached == null) {
            cached = ((SkyIslandChunkGenerator)(Object)this).embeddedSettings()
                    .orElseThrow(() -> new IllegalStateException("Create Sky requires embedded island settings"))
                    .toRuntimeSettings();
            resolvedSettingsCache = cached;
        }
        result.setReturnValue(cached);
    }

    @Inject(method = "getIslandField", at = @At("RETURN"))
    private void configure(RandomState random, CallbackInfoReturnable<IslandField> result) {
        if (createSky$isEnabled()) {
            ((ProfiledTerrain)(Object)result.getReturnValue()).createSky$configure(GenerationProfile.INITIAL);
        }
    }
}

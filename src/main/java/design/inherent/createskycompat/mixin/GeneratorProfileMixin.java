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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SkyIslandChunkGenerator.class, remap = false)
public abstract class GeneratorProfileMixin implements GeologicalGenerator {
    @Inject(method = "getBaseHeight", at = @At("HEAD"), cancellable = true)
    private void fluidHeightPredicate(int x, int z, net.minecraft.world.level.levelgen.Heightmap.Types type,
            net.minecraft.world.level.LevelHeightAccessor level, RandomState random, CallbackInfoReturnable<Integer> result) {
        if (!createSky$isEnabled()) return;
        var generator = (SkyIslandChunkGenerator)(Object)this;
        var field = createSky$field(random);
        if (((InteriorField)(Object)field).createSky$waterAt(x, z, generator.resolvedSkyIslandSettings()).sources().isEmpty()) return;
        var column = generator.getBaseColumn(x, z, level, random);
        for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
            if (type.isOpaque().test(column.getBlock(y))) { result.setReturnValue(y + 1); return; }
        }
        result.setReturnValue(level.getMinBuildHeight());
    }
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
            cached = GenerationConfiguration.resolve(cached);
            resolvedSettingsCache = cached;
        }
        result.setReturnValue(cached);
    }

    @Inject(method = "getIslandField", at = @At("RETURN"))
    private void configure(RandomState random, CallbackInfoReturnable<IslandField> result) {
        if (createSky$isEnabled()) {
            ((ProfiledTerrain)(Object)result.getReturnValue()).createSky$configure(GenerationConfiguration.profile());
            var noise = ((SkyIslandChunkGenerator)(Object)this).generatorSettings().value();
            ((VeinField)(Object)result.getReturnValue()).createSky$configureVeins(random, noise);
            ((InteriorField)(Object)result.getReturnValue()).createSky$configureInteriors(random, InteriorConfiguration.profile());
        }
    }
}

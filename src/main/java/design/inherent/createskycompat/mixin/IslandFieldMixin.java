package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import java.util.List;
import java.util.ArrayList;
import design.inherent.createskycompat.placement.VillageAnchors;
import design.inherent.createskycompat.placement.VillagePlan;
import design.inherent.createskycompat.terrain.ProfiledTerrain;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandClusterSampler;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDescriptorFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IslandField.class, remap = false)
public abstract class IslandFieldMixin implements ProfiledTerrain, GeologicalField, VillageAnchors, VeinField, InteriorField {
    @Shadow @Final private IslandClusterSampler clusterSampler;
    @Shadow @Final private IslandDescriptorFactory descriptorFactory;
    @Shadow @Final private org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandDensityEvaluator densityEvaluator;
    @Shadow @Final private org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandColumnResolver columnResolver;
    @Unique private volatile GenerationProfile createSky$profile;
    @Unique private volatile IslandVeins createSky$veins;
    @Unique private boolean createSky$veinsConfigured;
    @Unique private boolean createSky$interiorsConfigured;
    @Unique private IslandWaters createSky$waters;
    @Override public IslandWaters createSky$waters() { return createSky$waters; }
    @Override public WaterColumn createSky$waterAt(int x, int z, SkyIslandSettings settings) {
        return ((LayeredColumn)(Object)sampleColumnData(x, z, settings)).createSky$water();
    }
    @Shadow abstract IslandField.ColumnData sampleColumnData(int x, int z, SkyIslandSettings settings);
    @Shadow private IslandField.ColumnData resolveColumnData(int x, int z, SkyIslandSettings settings) { throw new AssertionError(); }
    @Override public IslandField.ColumnData createSky$planningColumn(int x, int z, SkyIslandSettings settings, TerrainStage stage) {
        if (stage == TerrainStage.COMPLETE) throw new IllegalArgumentException("Planning must select an earlier terrain stage");
        return stage.query(() -> resolveColumnData(x, z, settings));
    }
    @Override public synchronized void createSky$configureInteriors(net.minecraft.world.level.levelgen.RandomState random,
            InteriorProfile profile) {
        if (createSky$interiorsConfigured) return;
        createSky$waters = new IslandWaters(this, WaterConfiguration.profile(), OceanConfiguration.profile());
        ((InteriorResolver)(Object)columnResolver).createSky$interiors(new IslandInteriors(random, profile, createSky$waters));
        createSky$interiorsConfigured = true;
    }
    @Override public List<org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn> createSky$exteriorAt(
            int x, int z, SkyIslandSettings settings) {
        return ((LayeredColumn)(Object)sampleColumnData(x, z, settings)).createSky$exterior();
    }
    @Inject(method = "sampleColumn", at = @At("HEAD"), cancellable = true)
    private void actualTopInterval(int x, int z, SkyIslandSettings settings,
            CallbackInfoReturnable<org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn> result) {
        if (createSky$profile == null) return;
        var segments = sampleColumnData(x, z, settings).segments();
        result.setReturnValue(segments.isEmpty()
                ? org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn.EMPTY : segments.getFirst());
    }

    @Inject(method = "sampleDensity", at = @At("RETURN"), cancellable = true)
    private void actualDensity(int x, int y, int z, SkyIslandSettings settings, CallbackInfoReturnable<Double> result) {
        if (createSky$profile == null || result.getReturnValue() <= 0) return;
        if (sampleColumnData(x, z, settings).segments().stream().noneMatch(segment -> segment.contains(y)))
            result.setReturnValue(0.0);
    }
    @Override public synchronized void createSky$configureVeins(net.minecraft.world.level.levelgen.RandomState random,
            net.minecraft.world.level.levelgen.NoiseGeneratorSettings settings) {
        if (createSky$veinsConfigured) return;
        createSky$veins = settings.oreVeinsEnabled() ? new IslandVeins(random, settings) : null;
        createSky$veinsConfigured = true;
    }
    @Override public IslandVeins createSky$veins() { return createSky$veins; }

    @Override public synchronized void createSky$configure(GenerationProfile profile) {
        if (createSky$profile != null && createSky$profile.equals(profile)) return;
        if (createSky$profile != null) {
            throw new IllegalStateException("Cannot change a populated island field's generation identity");
        }
        ((ProfiledTerrain)(Object)clusterSampler).createSky$configure(profile);
        ((ProfiledTerrain)(Object)descriptorFactory).createSky$configure(profile);
        ((ProfiledTerrain)(Object)densityEvaluator).createSky$configure(profile);
        ((ProfiledTerrain)(Object)columnResolver).createSky$configure(profile);
        createSky$profile = profile;
    }
    @Override public GenerationProfile createSky$profile() { return createSky$profile; }

    @Shadow public abstract IslandField.IslandDescriptor sampleDominantIslandDescriptor(int x, int z, SkyIslandSettings settings);
    @Shadow private List<IslandField.IslandDescriptor> descriptorsForCell(int x, int z, SkyIslandSettings settings) { throw new AssertionError(); }

    @Override public List<VillagePlan.Anchor> createSky$anchorsNear(int x, int z, SkyIslandSettings settings) {
        if (createSky$profile == null) return List.of();
        int cx = Math.floorDiv(x, createSky$profile.spacing()), cz = Math.floorDiv(z, createSky$profile.spacing());
        var anchors = new ArrayList<VillagePlan.Anchor>();
        // Maximum configured village reach includes layout reservation and native SKY retries.
        int range = createSky$profile.lookupCells(createSky$profile.anchorMaxRadius() + 2048 + 320);
        for (int dx = -range; dx <= range; dx++) for (int dz = -range; dz <= range; dz++) {
            var descriptors = descriptorsForCell(cx + dx, cz + dz, settings);
            if (descriptors.isEmpty()) continue;
            var anchor = descriptors.getFirst();
            anchors.add(new VillagePlan.Anchor(anchor.seed(), anchor.centerX(), anchor.centerZ(), anchor.maxRadius(),
                    anchor.centerY() + anchor.plateauHeight()));
        }
        return List.copyOf(anchors);
    }

    @Override public GeologicalFrame createSky$frameAt(int x, int z, SkyIslandSettings settings) {
        if (createSky$profile == null) return null;
        var owner = sampleDominantIslandDescriptor(x, z, settings);
        if (owner == null) return null;
        var anchor = owner;
        if (owner.family() != IslandField.IslandFamily.ANCHOR_PLATEAU) {
            var membership = (ClusterField)this;
            var cell = membership.createSky$owner(owner, settings);
            anchor = membership.createSky$members(cell.x(), cell.z(), settings).getFirst();
        }
        return new GeologicalFrame(anchor.centerY() - anchor.cliffDepth(), anchor.centerY() + anchor.plateauHeight() + 1);
    }

    @ModifyConstant(method = {"sampleDensity", "collectSnapshotCandidates"}, constant = @Constant(intValue = 3), require = 8)
    private int searchRange(int original) { return createSky$profile == null ? original : createSky$profile.lookupCells(); }

    @Inject(method = "effectiveClusterSpacing", at = @At("HEAD"), cancellable = true)
    private void spacing(SkyIslandSettings settings, CallbackInfoReturnable<Integer> result) {
        if (createSky$profile != null) result.setReturnValue(createSky$profile.spacing());
    }
    @Inject(method = "scanBottom", at = @At("HEAD"), cancellable = true)
    private void bottom(SkyIslandSettings settings, CallbackInfoReturnable<Integer> result) {
        if (createSky$profile != null) result.setReturnValue(GenerationProfile.MIN_Y);
    }
    @Inject(method = "scanTop", at = @At("HEAD"), cancellable = true)
    private void top(SkyIslandSettings settings, CallbackInfoReturnable<Integer> result) {
        if (createSky$profile != null) result.setReturnValue(GenerationProfile.MAX_Y - 1);
    }
}

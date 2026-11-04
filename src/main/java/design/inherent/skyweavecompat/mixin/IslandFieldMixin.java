package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.*;
import java.util.List;
import java.util.ArrayList;
import design.inherent.skyweavecompat.placement.VillageAnchors;
import design.inherent.skyweavecompat.placement.VillagePlan;
import design.inherent.skyweavecompat.terrain.ProfiledTerrain;
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
    @Unique private volatile GenerationProfile skyweave$profile;
    @Unique private volatile IslandVeins skyweave$veins;
    @Unique private boolean skyweave$veinsConfigured;
    @Unique private boolean skyweave$interiorsConfigured;
    @Unique private IslandWaters skyweave$waters;
    @Override public IslandWaters skyweave$waters() { return skyweave$waters; }
    @Override public WaterColumn skyweave$waterAt(int x, int z, SkyIslandSettings settings) {
        return ((LayeredColumn)(Object)sampleColumnData(x, z, settings)).skyweave$water();
    }
    @Shadow abstract IslandField.ColumnData sampleColumnData(int x, int z, SkyIslandSettings settings);
    @Shadow private IslandField.ColumnData resolveColumnData(int x, int z, SkyIslandSettings settings) { throw new AssertionError(); }
    @Override public IslandField.ColumnData skyweave$planningColumn(int x, int z, SkyIslandSettings settings, TerrainStage stage) {
        if (stage == TerrainStage.COMPLETE) throw new IllegalArgumentException("Planning must select an earlier terrain stage");
        return stage.query(() -> resolveColumnData(x, z, settings));
    }
    @Override public synchronized void skyweave$configureInteriors(net.minecraft.world.level.levelgen.RandomState random,
            InteriorProfile profile) {
        if (skyweave$interiorsConfigured) return;
        skyweave$waters = new IslandWaters(this, WaterConfiguration.profile(), OceanConfiguration.profile());
        ((InteriorResolver)(Object)columnResolver).skyweave$interiors(new IslandInteriors(random, profile, skyweave$waters));
        skyweave$interiorsConfigured = true;
    }
    @Override public List<org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn> skyweave$exteriorAt(
            int x, int z, SkyIslandSettings settings) {
        return ((LayeredColumn)(Object)sampleColumnData(x, z, settings)).skyweave$exterior();
    }
    @Inject(method = "sampleColumn", at = @At("HEAD"), cancellable = true)
    private void actualTopInterval(int x, int z, SkyIslandSettings settings,
            CallbackInfoReturnable<org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn> result) {
        if (skyweave$profile == null) return;
        var segments = sampleColumnData(x, z, settings).segments();
        result.setReturnValue(segments.isEmpty()
                ? org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn.EMPTY : segments.getFirst());
    }

    @Inject(method = "sampleDensity", at = @At("RETURN"), cancellable = true)
    private void actualDensity(int x, int y, int z, SkyIslandSettings settings, CallbackInfoReturnable<Double> result) {
        if (skyweave$profile == null || result.getReturnValue() <= 0) return;
        if (sampleColumnData(x, z, settings).segments().stream().noneMatch(segment -> segment.contains(y)))
            result.setReturnValue(0.0);
    }
    @Override public synchronized void skyweave$configureVeins(net.minecraft.world.level.levelgen.RandomState random,
            net.minecraft.world.level.levelgen.NoiseGeneratorSettings settings) {
        if (skyweave$veinsConfigured) return;
        skyweave$veins = settings.oreVeinsEnabled() ? new IslandVeins(random, settings) : null;
        skyweave$veinsConfigured = true;
    }
    @Override public IslandVeins skyweave$veins() { return skyweave$veins; }

    @Override public synchronized void skyweave$configure(GenerationProfile profile) {
        if (skyweave$profile != null && skyweave$profile.equals(profile)) return;
        if (skyweave$profile != null) {
            throw new IllegalStateException("Cannot change a populated island field's generation identity");
        }
        ((ProfiledTerrain)(Object)clusterSampler).skyweave$configure(profile);
        ((ProfiledTerrain)(Object)descriptorFactory).skyweave$configure(profile);
        ((ProfiledTerrain)(Object)densityEvaluator).skyweave$configure(profile);
        ((ProfiledTerrain)(Object)columnResolver).skyweave$configure(profile);
        skyweave$profile = profile;
    }
    @Override public GenerationProfile skyweave$profile() { return skyweave$profile; }

    @Shadow public abstract IslandField.IslandDescriptor sampleDominantIslandDescriptor(int x, int z, SkyIslandSettings settings);
    @Shadow private List<IslandField.IslandDescriptor> descriptorsForCell(int x, int z, SkyIslandSettings settings) { throw new AssertionError(); }

    @Override public List<VillagePlan.Anchor> skyweave$anchorsNear(int x, int z, SkyIslandSettings settings) {
        if (skyweave$profile == null) return List.of();
        int cx = Math.floorDiv(x, skyweave$profile.spacing()), cz = Math.floorDiv(z, skyweave$profile.spacing());
        var anchors = new ArrayList<VillagePlan.Anchor>();
        // Maximum configured village reach includes layout reservation and native SKY retries.
        int range = skyweave$profile.lookupCells(skyweave$profile.anchorMaxRadius() + 2048 + 320);
        for (int dx = -range; dx <= range; dx++) for (int dz = -range; dz <= range; dz++) {
            var descriptors = descriptorsForCell(cx + dx, cz + dz, settings);
            if (descriptors.isEmpty()) continue;
            var anchor = descriptors.getFirst();
            anchors.add(new VillagePlan.Anchor(anchor.seed(), anchor.centerX(), anchor.centerZ(), anchor.maxRadius(),
                    anchor.centerY() + anchor.plateauHeight()));
        }
        return List.copyOf(anchors);
    }

    @Override public GeologicalFrame skyweave$frameAt(int x, int z, SkyIslandSettings settings) {
        if (skyweave$profile == null) return null;
        var owner = sampleDominantIslandDescriptor(x, z, settings);
        if (owner == null) return null;
        var anchor = owner;
        if (owner.family() != IslandField.IslandFamily.ANCHOR_PLATEAU) {
            var membership = (ClusterField)this;
            var cell = membership.skyweave$owner(owner, settings);
            anchor = membership.skyweave$members(cell.x(), cell.z(), settings).getFirst();
        }
        return new GeologicalFrame(anchor.centerY() - anchor.cliffDepth(), anchor.centerY() + anchor.plateauHeight() + 1);
    }

    @ModifyConstant(method = {"sampleDensity", "collectSnapshotCandidates"}, constant = @Constant(intValue = 3), require = 8)
    private int searchRange(int original) { return skyweave$profile == null ? original : skyweave$profile.lookupCells(); }

    @Inject(method = "effectiveClusterSpacing", at = @At("HEAD"), cancellable = true)
    private void spacing(SkyIslandSettings settings, CallbackInfoReturnable<Integer> result) {
        if (skyweave$profile != null) result.setReturnValue(skyweave$profile.spacing());
    }
    @Inject(method = "scanBottom", at = @At("HEAD"), cancellable = true)
    private void bottom(SkyIslandSettings settings, CallbackInfoReturnable<Integer> result) {
        if (skyweave$profile != null) result.setReturnValue(GenerationProfile.MIN_Y);
    }
    @Inject(method = "scanTop", at = @At("HEAD"), cancellable = true)
    private void top(SkyIslandSettings settings, CallbackInfoReturnable<Integer> result) {
        if (skyweave$profile != null) result.setReturnValue(GenerationProfile.MAX_Y - 1);
    }
}

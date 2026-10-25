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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IslandField.class, remap = false)
public abstract class IslandFieldMixin implements ProfiledTerrain, GeologicalField, VillageAnchors {
    @Shadow @Final private IslandClusterSampler clusterSampler;
    @Shadow @Final private IslandDescriptorFactory descriptorFactory;
    @Unique private volatile GenerationProfile createSky$profile;

    @Override public synchronized void createSky$configure(GenerationProfile profile) {
        if (createSky$profile != null && createSky$profile.equals(profile)) return;
        if (createSky$profile != null) {
            throw new IllegalStateException("Cannot change a populated island field's generation identity");
        }
        ((ProfiledTerrain)(Object)clusterSampler).createSky$configure(profile);
        ((ProfiledTerrain)(Object)descriptorFactory).createSky$configure(profile);
        createSky$profile = profile;
    }
    @Override public GenerationProfile createSky$profile() { return createSky$profile; }

    @Shadow public abstract IslandField.IslandDescriptor sampleDominantIslandDescriptor(int x, int z, SkyIslandSettings settings);
    @Shadow private List<IslandField.IslandDescriptor> descriptorsForCell(int x, int z, SkyIslandSettings settings) { throw new AssertionError(); }

    @Override public List<VillagePlan.Anchor> createSky$anchorsNear(int x, int z, SkyIslandSettings settings) {
        if (createSky$profile == null) return List.of();
        int cx = Math.floorDiv(x, createSky$profile.spacing()), cz = Math.floorDiv(z, createSky$profile.spacing());
        var anchors = new ArrayList<VillagePlan.Anchor>();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            var descriptors = descriptorsForCell(cx + dx, cz + dz, settings);
            if (descriptors.isEmpty()) continue;
            var anchor = descriptors.getFirst();
            anchors.add(new VillagePlan.Anchor(anchor.seed(), anchor.centerX(), anchor.centerZ(), anchor.maxRadius()));
        }
        return List.copyOf(anchors);
    }

    @Override public GeologicalFrame createSky$frameAt(int x, int z, SkyIslandSettings settings) {
        if (createSky$profile == null) return null;
        var owner = sampleDominantIslandDescriptor(x, z, settings);
        if (owner == null) return null;
        var anchor = owner;
        if (owner.family() != IslandField.IslandFamily.ANCHOR_PLATEAU) {
            int cx = Math.floorDiv(x, createSky$profile.spacing()), cz = Math.floorDiv(z, createSky$profile.spacing());
            anchor = null;
            // Same bounded cells as terrain lookup; no world/chunk reads or growing owner map.
            outer: for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                var candidates = descriptorsForCell(cx + dx, cz + dz, settings);
                if (candidates.contains(owner)) { anchor = candidates.getFirst(); break outer; }
            }
            if (anchor == null) throw new IllegalStateException("Island has no cluster anchor");
        }
        return new GeologicalFrame(anchor.centerY() - anchor.cliffDepth(), anchor.centerY() + anchor.plateauHeight() + 1);
    }

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

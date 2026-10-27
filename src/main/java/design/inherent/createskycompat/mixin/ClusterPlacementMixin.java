package design.inherent.createskycompat.mixin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import design.inherent.createskycompat.terrain.*;
import java.util.*;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField.*;
import org.sathrek.sky_archipelago.worldgen.generator.field.biomepolicy.BiomeIslandSpawnPolicy;
import org.sathrek.sky_archipelago.worldgen.generator.field.cache.ClusterCacheKey;
import org.sathrek.sky_archipelago.worldgen.generator.field.cache.IslandFieldCaches;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandClusterSampler;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.IslandNoise;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Candidates are cached independently from decisions, so rejection never recurses. */
@Mixin(value = IslandField.class, remap = false)
public abstract class ClusterPlacementMixin implements ClusterField {
    @Shadow @Final private long layoutSeed;
    @Shadow @Final private IslandNoise noise;
    @Shadow @Final private IslandClusterSampler clusterSampler;
    @Shadow @Final private BiomeTerrainSampler biomeTerrainSampler;
    @Shadow @Final private BiomeIslandSpawnPolicy biomeIslandSpawnPolicy;
    @Shadow @Final private IslandFieldCaches caches;
    @Shadow private ClusterDescriptor sampleCluster(int x, int z, SkyIslandSettings settings) { throw new AssertionError(); }
    @Shadow private List<IslandDescriptor> buildClusterIslands(ClusterDescriptor cluster, SkyIslandSettings settings) { throw new AssertionError(); }
    @Unique private final Cache<ClusterCacheKey, Optional<ClusterDescriptor>> createSky$candidates = CacheBuilder.newBuilder().maximumSize(8192).build();
    @Unique private final Cache<ClusterCacheKey, Boolean> createSky$decisions = CacheBuilder.newBuilder().maximumSize(8192).build();

    @Unique private GenerationProfile createSky$layoutProfile() { return ((ProfiledTerrain)this).createSky$profile(); }

    @Unique private ClusterDescriptor createSky$candidate(int x, int z, SkyIslandSettings settings) {
        var key = caches.clusterKey(x, z, settings);
        var cached = createSky$candidates.getIfPresent(key);
        if (cached != null) return cached.orElse(null);
        ClusterDescriptor candidate = null;
        var profile = createSky$layoutProfile();
        if (noise.sample01(x, z, 101) < profile.activation()) {
            var sampled = clusterSampler.sampleClusterDescriptor(x, z, layoutSeed, profile.spacing(), settings, 337);
            // Admission follows the displaced anchor and its surface datum, not the old cell center.
            var biome = biomeTerrainSampler.sample(sampled.centerX(), sampled.centerY() + profile.anchorPlateau(), sampled.centerZ());
            if (biomeIslandSpawnPolicy.allowIslandSpawn(biome, settings)) candidate = sampled;
        }
        createSky$candidates.put(key, Optional.ofNullable(candidate));
        return candidate;
    }

    @Unique private ClusterLayout.Candidate createSky$supports(ClusterDescriptor cluster, SkyIslandSettings settings) {
        return new ClusterLayout.Candidate(new ClusterLayout.Cell(cluster.cellX(), cluster.cellZ()),
                IslandNoise.mix(layoutSeed + 809, cluster.cellX(), cluster.cellZ()),
                buildClusterIslands(cluster, settings).stream().map(island -> new ClusterLayout.Support(
                        island.centerX(), island.centerZ(), GenerationProfile.supportRadius(island.maxRadius()))).toList());
    }

    @Inject(method = "sampleCluster", at = @At("HEAD"), cancellable = true)
    private void accepted(int x, int z, SkyIslandSettings settings, CallbackInfoReturnable<ClusterDescriptor> result) {
        var profile = createSky$layoutProfile();
        if (profile == null) return;
        var candidate = createSky$candidate(x, z, settings);
        if (candidate == null) { result.setReturnValue(null); return; }
        var key = caches.clusterKey(x, z, settings);
        var accepted = createSky$decisions.getIfPresent(key);
        if (accepted == null) {
            accepted = ClusterLayout.accepts(createSky$supports(candidate, settings), profile, (cx, cz) -> {
                var other = createSky$candidate(cx, cz, settings);
                return other == null ? null : createSky$supports(other, settings);
            });
            createSky$decisions.put(key, accepted);
        }
        result.setReturnValue(accepted ? candidate : null);
    }

    @Override public List<IslandDescriptor> createSky$members(int x, int z, SkyIslandSettings settings) {
        var cluster = sampleCluster(x, z, settings);
        return cluster == null ? List.of() : buildClusterIslands(cluster, settings);
    }

    @Override public ClusterLayout.Cell createSky$owner(IslandDescriptor descriptor, SkyIslandSettings settings) {
        var profile = createSky$layoutProfile();
        int cx = Math.floorDiv(descriptor.centerX(), profile.spacing());
        int cz = Math.floorDiv(descriptor.centerZ(), profile.spacing());
        int range = profile.lookupCells();
        for (int dx = -range; dx <= range; dx++) for (int dz = -range; dz <= range; dz++) {
            if (createSky$members(cx + dx, cz + dz, settings).contains(descriptor)) return new ClusterLayout.Cell(cx + dx, cz + dz);
        }
        throw new IllegalStateException("Island has no accepted cluster owner");
    }

    @Inject(method = "collectIslandPreviews", at = @At("HEAD"), cancellable = true)
    private void previews(int x, int z, int radius, SkyIslandSettings settings, boolean intersecting,
            CallbackInfoReturnable<List<IslandPreview>> result) {
        var profile = createSky$layoutProfile();
        if (profile == null) return;
        int range = profile.lookupCells(), spacing = profile.spacing();
        var previews = new ArrayList<IslandPreview>();
        for (int cx = Math.floorDiv(x - radius, spacing) - range; cx <= Math.floorDiv(x + radius, spacing) + range; cx++) {
            for (int cz = Math.floorDiv(z - radius, spacing) - range; cz <= Math.floorDiv(z + radius, spacing) + range; cz++) {
                var cluster = sampleCluster(cx, cz, settings);
                if (cluster == null) continue;
                for (var island : buildClusterIslands(cluster, settings)) {
                    int reach = GenerationProfile.supportRadius(island.maxRadius());
                    long dx = (long)island.centerX() - x, dz = (long)island.centerZ() - z;
                    long eligible = (long)radius + (intersecting ? reach : 0);
                    if (dx * dx + dz * dz <= eligible * eligible) previews.add(new IslandPreview(island.archetype(),
                            island.family(), cluster.heightBand(), island.centerX(), island.centerY(), island.centerZ(),
                            island.maxRadius(), island.plateauHeight(), island.hangDepth(), reach));
                }
            }
        }
        previews.sort(Comparator.comparingInt(IslandPreview::x).thenComparingInt(IslandPreview::z)
                .thenComparingInt(IslandPreview::radius).thenComparingInt(IslandPreview::y));
        result.setReturnValue(previews);
    }
}

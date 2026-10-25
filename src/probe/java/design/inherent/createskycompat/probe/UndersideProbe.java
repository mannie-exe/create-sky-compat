package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.terrain.*;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.*;
import org.sathrek.sky_archipelago.worldgen.generator.field.internal.*;

/** Native regression for the reported underside discontinuity, without a saved-world mount. */
final class UndersideProbe {
    private UndersideProbe() {}
    private static Object member(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    static JsonObject run(IslandField field, SkyIslandSettings settings) throws Exception {
        var noise = (IslandNoise)member(field, "noise");
        var sampler = (IslandShapeSampler)member(field, "shapeSampler");
        var applied = (IslandDensityEvaluator)member(field, "densityEvaluator");
        var nativeEvaluator = new IslandDensityEvaluator(noise, sampler,
                (int)member(applied, "topographySalt"), (int)member(applied, "erosionSalt"), (int)member(applied, "detailSalt"));
        var biomes = (IslandField.BiomeTerrainSampler)member(field, "biomeTerrainSampler");
        var descriptor = new IslandField.IslandDescriptor(IslandField.IslandFamily.ANCHOR_PLATEAU,
                1964, 349, 2015, 332, 344, 344, 0.7764812650321536, IslandShapeArchetype.CRESCENT,
                28, 290, 48, 20, -2, 115, 104, 0.5243334963094053, 0, 0, 0, -6563795249267109833L);
        var horizontal = sampler.sampleHorizontal(descriptor, 1800, 1928, settings);
        var terrain = biomes.sample(1800, 349, 1928).terrainProfile();
        var control = nativeEvaluator.prepareDensity(descriptor, horizontal, 1800, 1928, settings, terrain);
        var corrected = applied.prepareDensity(descriptor, horizontal, 1800, 1928, settings, terrain);
        double nativeStep = nativeEvaluator.samplePreparedDensity(control, 44) - nativeEvaluator.samplePreparedDensity(control, 43);
        double correctedStep = applied.samplePreparedDensity(corrected, 44) - applied.samplePreparedDensity(corrected, 43);
        if (nativeStep < 0.55 || Math.abs(correctedStep) > 0.1) {
            throw new AssertionError("Native cutoff reproduction or smooth replacement failed: " + nativeStep + ", " + correctedStep);
        }
        var row = new JsonObject(); row.addProperty("nativeStep", nativeStep); row.addProperty("correctedStep", correctedStep);
        var samples = new JsonArray();
        for (int y = 20; y <= 100; y++) {
            var sample = new JsonObject(); sample.addProperty("y", y);
            sample.addProperty("native", nativeEvaluator.samplePreparedDensity(control, y));
            sample.addProperty("corrected", applied.samplePreparedDensity(corrected, y)); samples.add(sample);
        }
        row.add("samples", samples);
        row.addProperty("evidence", "Pinned old spawn descriptor and native density; the new terrain uses separately randomized descriptors");
        return row;
    }
}

package design.inherent.skyweavecompat.terrain;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.config.settings.TerrainSettings;

/** Installation defaults, captured immutably by each field and checked against the saved world. */
public final class GenerationConfiguration {
    public static final String FILE = "skyweave_compat-generation.json";
    private static volatile GenerationProfile profile = GenerationProfile.INITIAL;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private GenerationConfiguration() {}

    public static GenerationProfile profile() { return profile; }

    /** The preset retains biome, archetype and structure rules; this profile owns geometry controls. */
    public static SkyIslandSettings resolve(SkyIslandSettings embedded) {
        var p = profile();
        var t = embedded.terrain();
        if (t.terrainReliefScale() < 0 || t.terrainReliefScale() > 1) {
            throw new IllegalArgumentException("Skyweave's verified terrain envelope requires terrain_relief_scale between 0 and 1");
        }
        var terrain = new TerrainSettings(p.activation(), p.anchorMinRadius(), p.anchorMaxRadius(), t.islandSize(),
                p.safeMinimumY(p.anchorMinHeight()), p.safeMaximumY(), t.maxIslandThicknessBlocks(),
                t.lowBandWeight(), t.midHighBandWeight(), t.veryHighBandWeight(), t.lowBandCenterOffset(), t.veryHighBandCenterOffset(),
                // Upstream's serialized spacing is capped at 2000. IslandFieldMixin owns the actual grid.
                t.spacing(),
                t.terrainReliefScale(), t.archetypes(), t.ocean());
        return new SkyIslandSettings(terrain, embedded.structureSupport(), embedded.mediumGround(), embedded.smallGround(),
                embedded.defaultSurface(), embedded.advanced(), RiverConfiguration.settings());
    }

    public static void load(Path directory) {
        Path path = directory.resolve(FILE);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(directory);
                Files.writeString(path, JSON.toJson(GenerationProfile.INITIAL) + "\n", StandardOpenOption.CREATE_NEW);
            }
            profile = parse(Files.readString(path));
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Cannot load " + path + ": " + failure.getMessage(), failure);
        }
    }

    public static GenerationProfile parse(String text) {
        return design.inherent.skyweavecompat.configuration.ProfileJson.generation(text);
    }
}

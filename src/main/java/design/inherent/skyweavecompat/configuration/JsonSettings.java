package design.inherent.skyweavecompat.configuration;

import com.google.gson.GsonBuilder;
import design.inherent.skyweavecompat.bluemap.BlueMapSettings;
import design.inherent.skyweavecompat.terrain.*;
import java.util.List;
import java.util.function.Consumer;

/** Only these owned files are editable. Validators are shared with runtime loaders. */
public final class JsonSettings {
    public record Definition(String filename, String help, String defaults, Consumer<String> validate) {}
    private static String json(Object value) { return new GsonBuilder().setPrettyPrinting().create().toJson(value) + "\n"; }
    public static final List<Definition> FILES = List.of(
            new Definition("skyweave_compat-generation.json", "generation", json(GenerationProfile.INITIAL), ProfileJson::generation),
            new Definition("skyweave_compat-interiors.json", "interiors", json(InteriorProfile.INITIAL), InteriorConfiguration::parse),
            new Definition("skyweave_compat-water.json", "water", json(WaterProfile.INITIAL), WaterConfiguration::parse),
            new Definition("skyweave_compat-oceans.json", "oceans", json(OceanProfile.INITIAL), OceanConfiguration::parse),
            new Definition(BlueMapSettings.FILE, "bluemap", json(BlueMapSettings.DEFAULT), BlueMapSettings::parse));

    public static Definition find(String filename) {
        return FILES.stream().filter(file -> file.filename().equals(filename)).findFirst().orElse(null);
    }
    private JsonSettings() {}
}

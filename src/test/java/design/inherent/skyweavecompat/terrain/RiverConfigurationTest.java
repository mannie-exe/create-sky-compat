package design.inherent.skyweavecompat.terrain;

import com.google.gson.*;
import design.inherent.skyweavecompat.configuration.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RiverConfigurationTest {
    @TempDir Path directory;
    private JsonSettings.Definition definition() {
        var found = JsonSettings.find(RiverConfiguration.FILE);
        if (found != null) return found;
        var definition = RiverConfiguration.definition();
        JsonSettings.register(definition);
        return definition;
    }

    @Test void nestedNativeSettingsRoundTripWithoutOverwritingSiblingFields() throws Exception {
        var definition = definition();
        var file = directory.resolve(definition.filename());
        Files.writeString(file, definition.defaults());
        var edits = new JsonEdits(directory, definition);
        assertTrue(edits.keys().contains("anchors.minimum_width"));
        assertEquals(JsonEdits.Kind.STRING, edits.kind("temperate_biome"));
        edits.set("anchors.minimum_width", "14");
        assertThrows(RuntimeException.class, edits::save);
        assertEquals(definition.defaults(), Files.readString(file));
        edits.set("anchors.maximum_width", "16");
        edits.set("temperate_biome", "minecraft:stony_shore");
        edits.strings("excluded_biomes", List.of("minecraft:desert"));
        edits.save();
        var saved = RiverConfiguration.parse(Files.readString(file));
        assertEquals(14, saved.anchors().minimumWidth());
        assertEquals(16, saved.anchors().maximumWidth());
        assertEquals(6, saved.satellites().maximumWidth());
        assertEquals("minecraft:stony_shore", saved.temperateBiome().toString());
        assertEquals(1, saved.excludedBiomes().size());
        edits.reset("anchors.minimum_width");
        edits.reset("anchors.maximum_width");
        edits.save();
        assertEquals(6, RiverConfiguration.parse(Files.readString(file)).anchors().minimumWidth());
    }

    @Test void nativeCodecsDoNotSilentlyCoerceFractionalIntegersOrIgnoreUnknownSettings() {
        var tree = JsonParser.parseString(definition().defaults()).getAsJsonObject();
        tree.getAsJsonObject("anchors").addProperty("maximum_width", 12.5);
        assertThrows(IllegalArgumentException.class, () -> RiverConfiguration.parse(tree.toString()));
        tree.getAsJsonObject("anchors").addProperty("maximum_width", 12);
        tree.getAsJsonObject("anchors").addProperty("unknown", 1);
        assertThrows(IllegalArgumentException.class, () -> RiverConfiguration.parse(tree.toString()));
        assertThrows(IllegalArgumentException.class, () -> RiverConfiguration.parse("{\"enabled\":true,\"enabled\":false}"));
    }
}

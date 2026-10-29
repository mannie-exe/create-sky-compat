package design.inherent.createskycompat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FeatureSelectionTest {
    private static final Map<String, String> SUPPORTED = Map.ofEntries(
            Map.entry("sky_archipelago", "1.3.6"), Map.entry("skyvillages", "1.0.6"),
            Map.entry("architectury", "13.0.11"), Map.entry("sable", "2.0.6"),
            Map.entry("aeronautics_delivery_quests", "1.1.1"), Map.entry("distanthorizons", "3.3.3"),
            Map.entry("bluemap", "5.7"), Map.entry("analogaudio", "0.1.5-hotfix.1"),
            Map.entry("create", "6.0.10"), Map.entry("veil", "4.3.2"),
            Map.entry("iris", "1.8.14-beta.1+mc1.21.1"), Map.entry("sodium", "0.8.13+mc1.21.1"),
            Map.entry("aeronautics", "1.3.2"));

    private static FeatureSettings only(Feature... features) {
        var values = new EnumMap<Feature, Boolean>(Feature.class);
        for (var feature : Feature.values()) values.put(feature, false);
        for (var feature : features) values.put(feature, true);
        return new FeatureSettings(values);
    }

    @Test void noDependenciesSelectsNoHooksOnEitherSide() {
        for (boolean client : new boolean[]{true, false}) {
            var selection = FeatureSelection.select(new FeatureSettings(Map.of()), Map.of(), client);
            assertTrue(selection.active().isEmpty());
            assertTrue(selection.mixins(client).isEmpty());
        }
    }

    @Test void allDisabledIgnoresInstalledVersionsAndLegacyPatches() {
        var versions = new HashMap<String, String>();
        SUPPORTED.keySet().forEach(id -> versions.put(id, "unsupported"));
        versions.put("veil_iris_fix", "0.1.0");
        versions.put("sky_village_neighbors", "1.0");
        var selection = FeatureSelection.select(only(), versions, true);
        assertTrue(selection.active().isEmpty());
        assertTrue(selection.mixins(true).isEmpty());
    }

    @Test void independentFixesDoNotNeedTerrainOrItsClasses() {
        var selection = FeatureSelection.select(only(Feature.ANALOG_AUDIO, Feature.DH_HANDOFF, Feature.FRAMEBUFFER),
                Map.of("analogaudio", "0.1.5-hotfix.1", "create", "6.0.10", "distanthorizons", "3.3.3",
                        "veil", "4.3.2", "iris", "1.8.14-beta.1+mc1.21.1"), true);
        assertEquals(3, selection.active().size());
        assertEquals(3, selection.mixins(true).size());
        assertFalse(selection.enabled(Feature.TERRAIN));
    }

    @Test void serverNeverSelectsClientAdaptersOrValidatesTheirVersions() {
        var versions = new HashMap<>(SUPPORTED);
        versions.put("iris", "unsupported");
        var selection = FeatureSelection.select(new FeatureSettings(Map.of()), versions, false);
        assertFalse(selection.enabled(Feature.FRAMEBUFFER));
        assertFalse(selection.enabled(Feature.BALLOON));
        assertFalse(selection.enabled(Feature.MATERIAL_IDS));
        assertFalse(selection.enabled(Feature.MODEL_DATA));
        assertFalse(selection.enabled(Feature.SINGLE_BLOCK_PASSES));
        assertTrue(selection.mixins(false).stream().noneMatch(name -> name.startsWith("client.")));
        assertTrue(selection.enabled(Feature.BLUEMAP));
    }

    @Test void singleBlockPassesAreIndependentAndNeedTheirRendererStack() {
        var selection = FeatureSelection.select(only(Feature.SINGLE_BLOCK_PASSES), SUPPORTED, true);
        assertEquals(java.util.List.of("client.SingleBlockPassMixin"), selection.mixins(true));
        var missing = new HashMap<>(SUPPORTED);
        missing.remove("iris");
        assertTrue(FeatureSelection.select(only(Feature.SINGLE_BLOCK_PASSES), missing, true).active().isEmpty());
        missing.put("iris", "unsupported");
        assertTrue(FeatureSelection.select(only(), missing, true).active().isEmpty());
    }

    @Test void activeAdaptersRejectUnsupportedVersionsWithConfigurationRemedy() {
        var versions = new HashMap<>(SUPPORTED);
        versions.put("iris", "unsupported");
        var failure = assertThrows(IllegalStateException.class,
                () -> FeatureSelection.select(only(Feature.FRAMEBUFFER), versions, true));
        assertTrue(failure.getMessage().contains(Feature.FRAMEBUFFER.key));
        assertTrue(failure.getMessage().contains(FeatureSettings.FILE));
    }

    @Test void disabledPrerequisitesAreNotSilentlyEnabled() {
        var selection = FeatureSelection.select(only(Feature.PRESSURE, Feature.DELIVERY_CHUNKS), SUPPORTED, false);
        assertTrue(selection.active().isEmpty());
        assertTrue(selection.inactive().get(Feature.PRESSURE).contains(Feature.TERRAIN.key));
    }

    @Test void missingCounterpartDoesNotValidateAnInactiveAdapter() {
        var selection = FeatureSelection.select(only(Feature.ANALOG_AUDIO), Map.of("analogaudio", "future"), false);
        assertTrue(selection.active().isEmpty());
        assertEquals("missing create", selection.inactive().get(Feature.ANALOG_AUDIO));
    }

    @Test void oldFramebufferPatchConflictsOnlyWhenReplacementActive() {
        var versions = new HashMap<>(SUPPORTED);
        versions.put("veil_iris_fix", "0.1.0");
        assertThrows(IllegalStateException.class,
                () -> FeatureSelection.select(only(Feature.FRAMEBUFFER), versions, true));
        assertTrue(FeatureSelection.select(only(), versions, true).active().isEmpty());
    }

    @Test void deliverySwitchesSelectSeparateHooks() {
        for (var feature : new Feature[]{Feature.DELIVERY_DISTANCE, Feature.DELIVERY_CHUNKS, Feature.DELIVERY_LANDING}) {
            var selection = FeatureSelection.select(only(Feature.TERRAIN, feature), SUPPORTED, false);
            assertEquals(1, selection.mixins(false).stream().filter(name -> name.startsWith("Delivery")).count());
        }
    }

    @Test void settingsRejectMistakesAndKeepOperatorValues(@TempDir Path directory) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> FeatureSettings.read(new StringReader("terrain.preset=maybe")));
        assertThrows(IllegalArgumentException.class, () -> FeatureSettings.read(new StringReader("terrain.presett=false")));
        assertThrows(IllegalArgumentException.class, () -> FeatureSettings.read(new StringReader("terrain.preset=true\nterrain.preset=false")));
        var defaults = FeatureSettings.load(directory);
        assertTrue(defaults.enabled(Feature.TERRAIN));
        assertFalse(defaults.enabled(Feature.DH_CACHE));
        Files.writeString(directory.resolve(FeatureSettings.FILE), "terrain.preset=false\n");
        assertFalse(FeatureSettings.load(directory).enabled(Feature.TERRAIN));
        assertEquals("terrain.preset=false\n", Files.readString(directory.resolve(FeatureSettings.FILE)));
    }

    @Test void firstStartupRetainsVillageOptOutWithoutKeepingTwoAuthorities(@TempDir Path directory) throws Exception {
        var legacy = directory.resolve("create_sky_compat-common.toml");
        Files.writeString(legacy, "enabled = false # operator choice\n");
        assertFalse(FeatureSettings.load(directory).enabled(Feature.VILLAGE_CLEARANCE));
        Files.writeString(legacy, "enabled = true\n");
        assertFalse(FeatureSettings.load(directory).enabled(Feature.VILLAGE_CLEARANCE));
    }
}

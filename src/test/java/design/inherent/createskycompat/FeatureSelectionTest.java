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
    @Test void overviewNamesEveryConfigurableFeature() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/create_sky_compat/lang/en_us.json")) {
            assertNotNull(stream);
            var labels = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (var feature : Feature.values()) {
                var key = "create_sky_compat.configuration.feature." + feature.key;
                assertTrue(labels.has(key) && !labels.get(key).getAsString().isBlank(), key);
            }
        }
    }
    @Test void constructRecoveryRequiresBothTargetsAndSupportsIntegratedServers() {
        var settings = only(Feature.SABLE_GRAVES);
        for (boolean client : new boolean[]{true, false}) {
            assertTrue(FeatureSelection.select(settings, Map.of("sable", "2.0.6", "yigd", "2.0.13"), client)
                    .enabled(Feature.SABLE_GRAVES));
            assertTrue(FeatureSelection.select(settings, Map.of("sable", "2.0.6"), client).active().isEmpty());
            assertTrue(FeatureSelection.select(settings, Map.of("yigd", "2.0.13"), client).active().isEmpty());
            assertTrue(FeatureSelection.select(only(), Map.of("sable", "future", "yigd", "future"), client).active().isEmpty());
        }
    }
    @Test void radioTimelineNeedsOnlyTheAudioPairOnPhysicalClients() {
        var settings = only(Feature.AUDIO_TIMELINE);
        var versions = Map.of("analogaudio", "0.1.5-hotfix.1", "analoglib", "0.1.0");
        assertTrue(FeatureSelection.select(settings, versions, true).enabled(Feature.AUDIO_TIMELINE));
        assertTrue(FeatureSelection.select(settings, versions, false).active().isEmpty());
        assertTrue(FeatureSelection.select(settings, Map.of("analogaudio", "0.1.5-hotfix.1"), true).active().isEmpty());
        assertTrue(FeatureSelection.select(settings, Map.of("analoglib", "0.1.0"), true).active().isEmpty());
        assertTrue(FeatureSelection.select(only(), Map.of("analoglib", "future", "analogaudio", "future"), true).active().isEmpty());
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings,
                Map.of("analoglib", "future", "analogaudio", "0.1.5-hotfix.1"), true));
    }
    @Test void linkageRequiresItsCompletePairAndKeepsJadeIndependent() {
        var settings = only(Feature.LINKAGE_INTERACTIONS, Feature.LINKAGE_JADE);
        var versions = new HashMap<>(Map.of("aeronautics_utility_objects", "0.2.9", "create", "6.0.10",
                "sable", "2.0.6", "simulated", "1.3.2", "jade", "15.10.6+neoforge"));
        for (boolean client : new boolean[]{false, true}) {
            var selection = FeatureSelection.select(settings, versions, client);
            assertTrue(selection.enabled(Feature.LINKAGE_INTERACTIONS));
            assertTrue(selection.enabled(Feature.LINKAGE_JADE));
            assertEquals(client, selection.mixins(client).contains("client.LinkageRodInputMixin"));
        }
        versions.remove("jade");
        assertFalse(FeatureSelection.select(settings, versions, false).enabled(Feature.LINKAGE_JADE));
        assertTrue(FeatureSelection.select(settings, versions, false).enabled(Feature.LINKAGE_INTERACTIONS));
        versions.remove("simulated");
        assertTrue(FeatureSelection.select(settings, versions, false).active().isEmpty());
        versions.put("simulated", "future");
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings, versions, false));
        assertTrue(FeatureSelection.select(only(), versions, false).active().isEmpty());
        versions.remove("aeronautics_utility_objects");
        assertTrue(FeatureSelection.select(settings, versions, false).active().isEmpty());
        assertTrue(FeatureSelection.select(settings, Map.of(), true).active().isEmpty());
    }
    @Test void linearGuidanceRequiresItsApisAndAnActiveCarriageAdapter() {
        var versions = new HashMap<>(Map.of("linearbearing", "1.3.5", "create", "6.0.10",
                "sable", "2.0.6", "simulated", "1.3.2", "ponder", "1.0.87+mc1.21.1", "jade", "15.10.6+neoforge"));
        var settings = only(Feature.LINEAR_CARRIAGE, Feature.LINEAR_PONDER, Feature.LINEAR_JADE);
        assertTrue(FeatureSelection.select(settings, versions, true).enabled(Feature.LINEAR_PONDER));
        assertTrue(FeatureSelection.select(settings, versions, false).enabled(Feature.LINEAR_JADE));
        assertFalse(FeatureSelection.select(settings, versions, false).enabled(Feature.LINEAR_PONDER));
        assertTrue(FeatureSelection.select(only(Feature.LINEAR_PONDER, Feature.LINEAR_JADE), versions, true).active().isEmpty());
        versions.remove("jade");
        assertFalse(FeatureSelection.select(settings, versions, true).enabled(Feature.LINEAR_JADE));
        versions.put("jade", "future");
        assertFalse(FeatureSelection.select(only(Feature.LINEAR_CARRIAGE, Feature.LINEAR_PONDER), versions, true).enabled(Feature.LINEAR_JADE));
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings, versions, true));
        versions.remove("ponder");
        assertFalse(FeatureSelection.select(only(Feature.LINEAR_CARRIAGE, Feature.LINEAR_PONDER), versions, true).enabled(Feature.LINEAR_PONDER));
    }
    @Test void linearMachineryNeedsItsOwnApisAndSurvivesStandaloneFixCoexistence() {
        var settings = only(Feature.LINEAR_BOOTSTRAP, Feature.LINEAR_CARRIAGE, Feature.LINEAR_TRANSMISSION, Feature.LINEAR_GYRO);
        var versions = new HashMap<>(Map.of("linearbearing", "1.3.5", "linearbearingsfix", "1.0.3",
                "create", "6.0.10", "sable", "2.0.6", "simulated", "1.3.2"));
        for (boolean client : new boolean[]{false, true}) {
            var selection = FeatureSelection.select(settings, versions, client);
            assertEquals(java.util.Set.of(Feature.LINEAR_CARRIAGE, Feature.LINEAR_TRANSMISSION, Feature.LINEAR_GYRO), selection.active());
            assertEquals(8, selection.mixins(client).size());
        }
        versions.remove("simulated");
        var noSpringApi = FeatureSelection.select(settings, versions, false);
        assertFalse(noSpringApi.enabled(Feature.LINEAR_TRANSMISSION));
        assertFalse(noSpringApi.enabled(Feature.LINEAR_CARRIAGE));
        assertTrue(noSpringApi.enabled(Feature.LINEAR_GYRO));
        versions.remove("sable");
        assertTrue(FeatureSelection.select(settings, versions, false).active().isEmpty());
        versions.replaceAll((id, version) -> "future");
        assertTrue(FeatureSelection.select(only(), versions, false).active().isEmpty());
    }
    @Test void linearBootstrapIsOptionalAndDefersOnlyItsOverlappingRepairs() {
        var settings = only(Feature.LINEAR_BOOTSTRAP);
        var versions = new HashMap<>(Map.of("linearbearing", "1.3.5"));
        assertEquals(java.util.List.of("linear.LinearBootstrapMixin"),
                FeatureSelection.select(settings, versions, false).mixins(false));
        var client = FeatureSelection.select(settings, versions, true);
        assertTrue(client.enabled(Feature.LINEAR_BOOTSTRAP), "client still needs the gated loot pack");
        assertTrue(client.mixins(true).isEmpty(), "client initialization must remain unchanged");
        assertTrue(FeatureSelection.select(settings, Map.of(), false).active().isEmpty());
        versions.put("linearbearingsfix", "1.0.3");
        var coexist = FeatureSelection.select(settings, versions, false);
        assertTrue(coexist.active().isEmpty());
        assertEquals("provided by Linear Bearings Fix 1.0.3", coexist.inactive().get(Feature.LINEAR_BOOTSTRAP));
        versions.put("linearbearingsfix", "future");
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings, versions, false));
        versions.put("linearbearing", "future");
        assertTrue(FeatureSelection.select(only(), versions, false).active().isEmpty());
        versions.remove("linearbearingsfix");
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings, versions, false));
    }
    private static final Map<String, String> SUPPORTED = Map.ofEntries(
            Map.entry("sky_archipelago", "1.3.6"), Map.entry("skyvillages", "1.0.6"),
            Map.entry("architectury", "13.0.11"), Map.entry("sable", "2.0.6"),
            Map.entry("aeronautics_delivery_quests", "1.1.1"), Map.entry("distanthorizons", "3.3.3"),
            Map.entry("bluemap", "5.7"), Map.entry("analogaudio", "0.1.5-hotfix.1"),
            Map.entry("create", "6.0.10"), Map.entry("veil", "4.3.2"),
            Map.entry("iris", "1.8.14-beta.1+mc1.21.1"), Map.entry("sodium", "0.8.13+mc1.21.1"),
            Map.entry("sabledynlights", "2.0.1"), Map.entry("lambdynlights", "4.8.11+1.21.1"),
            Map.entry("flywheel", "1.0.6"), Map.entry("colorwheel", "1.3.0+mc1.21.1"),
            Map.entry("aeronautics", "1.3.2"), Map.entry("questlog", "3.5.0"));

    @Test void colorwheelBridgeRequiresItsOwnerAndRespectsAbsenceDisableAndSide() {
        var settings = only(Feature.MATERIAL_IDS, Feature.COLORWHEEL_MATERIALS);
        assertTrue(FeatureSelection.select(settings, SUPPORTED, true).enabled(Feature.COLORWHEEL_MATERIALS));
        assertFalse(FeatureSelection.select(only(Feature.COLORWHEEL_MATERIALS), SUPPORTED, true)
                .enabled(Feature.COLORWHEEL_MATERIALS));
        var versions = new HashMap<>(SUPPORTED);
        versions.remove("colorwheel");
        var missing = FeatureSelection.select(settings, versions, true);
        assertTrue(missing.enabled(Feature.MATERIAL_IDS));
        assertFalse(missing.enabled(Feature.COLORWHEEL_MATERIALS));
        versions.put("colorwheel", "future");
        assertFalse(FeatureSelection.select(only(Feature.MATERIAL_IDS), versions, true)
                .enabled(Feature.COLORWHEEL_MATERIALS));
        assertFalse(FeatureSelection.select(settings, versions, false).enabled(Feature.COLORWHEEL_MATERIALS));
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings, versions, true));
    }

    @Test void journalFeaturesNeedOnlyQuestlogAndRespectPhysicalSides() {
        var settings = only(Feature.QUESTLOG_INTERFACE, Feature.QUESTLOG_NOTIFICATIONS, Feature.QUESTLOG_REWARD_VALIDATION);
        var versions = Map.of("questlog", "3.5.3");
        assertEquals(3, FeatureSelection.select(settings, Map.of("questlog", "3.5.0"), true).active().size());
        assertEquals(3, FeatureSelection.select(settings, versions, true).active().size());
        var server = FeatureSelection.select(settings, versions, false);
        assertEquals(java.util.Set.of(Feature.QUESTLOG_REWARD_VALIDATION), server.active());
        assertEquals(java.util.List.of("questlog.RewardClaimMixin"), server.mixins(false));
        assertTrue(FeatureSelection.select(settings, Map.of(), true).active().isEmpty());
        assertTrue(FeatureSelection.select(only(), Map.of("questlog", "future"), true).active().isEmpty());
        assertThrows(IllegalStateException.class,
                () -> FeatureSelection.select(settings, Map.of("questlog", "future"), true));
        assertTrue(FeatureSelection.select(only(Feature.QUESTLOG_INTERFACE, Feature.QUESTLOG_NOTIFICATIONS),
                Map.of("questlog", "future"), false).active().isEmpty());
    }

    @Test void lightCleanupNeedsNeitherSableNorSodium() {
        var versions = Map.of("sabledynlights", "2.0.1", "lambdynlights", "4.8.11+1.21.1", "create", "6.0.10");
        assertEquals(8, FeatureSelection.select(only(Feature.LIGHTING_LIFECYCLE), versions, true).mixins(true).size());
        assertTrue(FeatureSelection.select(only(Feature.LIGHTING_LIFECYCLE), versions, false).active().isEmpty());
    }

    @Test void colorwheelSamplerRepairNeedsNeitherSableNorTerrain() {
        var settings = only(Feature.COLORWHEEL_SAMPLERS);
        var versions = new HashMap<>(Map.of("colorwheel", "1.3.0+mc1.21.1",
                "iris", "1.8.14-beta.1+mc1.21.1", "flywheel", "1.0.6"));
        assertEquals(java.util.List.of("client.ColorwheelSamplerMixin"),
                FeatureSelection.select(settings, versions, true).mixins(true));
        for (var missing : java.util.List.copyOf(versions.keySet())) {
            var incomplete = new HashMap<>(versions); incomplete.remove(missing);
            incomplete.replaceAll((id, version) -> "unsupported");
            assertTrue(FeatureSelection.select(settings, incomplete, true).active().isEmpty());
        }
        versions.replaceAll((id, version) -> "unsupported");
        assertTrue(FeatureSelection.select(only(), versions, true).active().isEmpty());
        assertTrue(FeatureSelection.select(settings, versions, false).active().isEmpty());
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings, versions, true));
    }

    @Test void sodiumLightingNeedsEveryBridgeButNotSableOrShaders() {
        var versions = new HashMap<>(Map.of("sabledynlights", "2.0.1", "lambdynlights", "4.8.11+1.21.1",
                "sodium", "0.8.13+mc1.21.1", "create", "6.0.10"));
        var settings = only(Feature.SODIUM_LIGHTING);
        assertEquals(java.util.List.of("client.SodiumLightingMixin"),
                FeatureSelection.select(settings, versions, true).mixins(true));
        for (var missing : java.util.List.copyOf(versions.keySet())) {
            var incomplete = new HashMap<>(versions); incomplete.remove(missing);
            incomplete.replaceAll((id, version) -> "unsupported");
            assertTrue(FeatureSelection.select(settings, incomplete, true).active().isEmpty());
        }
        versions.replaceAll((id, version) -> "unsupported");
        assertTrue(FeatureSelection.select(only(), versions, true).active().isEmpty());
        assertTrue(FeatureSelection.select(settings, versions, false).active().isEmpty());
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(settings, versions, true));
    }

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

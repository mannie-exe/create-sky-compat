package design.inherent.createskycompat;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedstoneSelectionTest {
    private FeatureSettings only(Feature... enabled) {
        var values = new EnumMap<Feature, Boolean>(Feature.class);
        for (var feature : Feature.values()) values.put(feature, false);
        for (var feature : enabled) values.put(feature, true);
        return new FeatureSettings(values);
    }
    @Test void guiNeedsBothTargetsAndKeepsShipIntegrationOptional() {
        var settings = only(Feature.LINK_GUI_INTERACTIONS, Feature.LINK_GUI_RELOCATION, Feature.LINK_GUI_SABLE);
        var versions = new java.util.HashMap<>(Map.of("create", "6.0.10", "createredstonelinkgui", "1.21.1-1.9.2"));
        for (boolean client : new boolean[]{false, true}) {
            var result = FeatureSelection.select(settings, versions, client);
            assertTrue(result.enabled(Feature.LINK_GUI_INTERACTIONS));
            assertTrue(result.enabled(Feature.LINK_GUI_RELOCATION));
            assertFalse(result.enabled(Feature.LINK_GUI_SABLE));
            assertEquals(client, result.mixins(client).contains("client.GuiRelocationScreenMixin"));
        }
        versions.put("sable", "2.0.6"); assertTrue(FeatureSelection.select(settings, versions, false).enabled(Feature.LINK_GUI_SABLE));
        versions.remove("create"); assertTrue(FeatureSelection.select(settings, versions, false).active().isEmpty());
        versions.put("create", "6.0.10"); versions.remove("createredstonelinkgui");
        assertTrue(FeatureSelection.select(settings, versions, true).active().isEmpty());
    }
    @Test void disabledGuiDoesNotConstrainUnreviewedTargets() {
        var future = Map.of("create", "future", "createredstonelinkgui", "future", "sable", "future");
        assertTrue(FeatureSelection.select(only(), future, true).active().isEmpty());
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(only(Feature.LINK_GUI_INTERACTIONS), future, true));
        var withoutAccess = FeatureSelection.select(only(Feature.LINK_GUI_SABLE), future, true);
        assertTrue(withoutAccess.active().isEmpty());
    }
    @Test void additionsHasIndependentRepairsAndPhysicalClientRendering() {
        var settings = only(Feature.REDSTONE_LOGIC, Feature.REDSTONE_SCRAPING, Feature.REDSTONE_WRENCH, Feature.REDSTONE_RENDERING);
        var versions = Map.of("create", "6.0.10", "create_redstone_additions", "1.0.5");
        for (boolean client : new boolean[]{false, true}) {
            var selection = FeatureSelection.select(settings, versions, client);
            assertTrue(selection.enabled(Feature.REDSTONE_LOGIC));
            assertTrue(selection.enabled(Feature.REDSTONE_SCRAPING));
            assertTrue(selection.enabled(Feature.REDSTONE_WRENCH));
            assertEquals(client, selection.enabled(Feature.REDSTONE_RENDERING));
        }
        for (String missing : versions.keySet()) {
            var absent = new java.util.HashMap<>(versions); absent.remove(missing);
            assertTrue(FeatureSelection.select(settings, absent, true).active().isEmpty());
        }
        var wrenchOnly = FeatureSelection.select(only(Feature.REDSTONE_WRENCH), versions, false);
        assertEquals(java.util.Set.of(Feature.REDSTONE_WRENCH), wrenchOnly.active());
        assertEquals(java.util.List.of("redstone.GateWrenchMixin"), wrenchOnly.mixins(false));
    }
    @Test void additionsVersionGateAppliesOnlyToEnabledPresentTargets() {
        var future = Map.of("create", "6.0.10", "create_redstone_additions", "1.0.6");
        assertTrue(FeatureSelection.select(only(), future, true).active().isEmpty());
        assertThrows(IllegalStateException.class, () -> FeatureSelection.select(only(Feature.REDSTONE_LOGIC), future, false));
    }
}

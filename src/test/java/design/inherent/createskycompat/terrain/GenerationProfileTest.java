package design.inherent.createskycompat.terrain;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenerationProfileTest {
    @Test void validatesCompleteClusterSeparationRatherThanCenterDistance() {
        var json = new Gson().toJsonTree(GenerationProfile.INITIAL).getAsJsonObject();
        json.addProperty("spacing", 2000);
        assertThrows(RuntimeException.class, () -> GenerationConfiguration.parse(json.toString()));
        json.addProperty("spacing", 4500);
        var profile = GenerationConfiguration.parse(json.toString());
        assertTrue(profile.spacing() - 2 * profile.jitter() - 2 * profile.clusterReach() >= profile.minimumClusterGap());
    }

    @Test void rejectsUnknownMissingFractionalAndImpossibleControls() {
        var json = new Gson().toJsonTree(GenerationProfile.INITIAL).getAsJsonObject();
        var unknown = json.deepCopy(); unknown.addProperty("typo", 42);
        assertThrows(RuntimeException.class, () -> GenerationConfiguration.parse(unknown.toString()));
        var missing = json.deepCopy(); missing.remove("activation");
        assertThrows(RuntimeException.class, () -> GenerationConfiguration.parse(missing.toString()));
        var fractional = json.deepCopy(); fractional.addProperty("centerMinY", 200.5);
        assertThrows(RuntimeException.class, () -> GenerationConfiguration.parse(fractional.toString()));
        var impossible = json.deepCopy(); impossible.addProperty("buildingHeadroom", 800);
        assertThrows(RuntimeException.class, () -> GenerationConfiguration.parse(impossible.toString()));
    }

    @Test void safetyMarginsNarrowPlacementWithoutShrinkingBodies() {
        var json = new Gson().toJsonTree(GenerationProfile.INITIAL).getAsJsonObject();
        json.addProperty("centerMinY", 100);
        json.addProperty("centerMaxY", 700);
        var profile = GenerationConfiguration.parse(json.toString());
        assertTrue(profile.safeMinimumY() - profile.anchorCliff() - 48 >= GenerationProfile.MIN_Y + profile.floorClearance());
        assertTrue(profile.safeMaximumY() + profile.anchorPlateau() + 48 <= GenerationProfile.MAX_Y - 1 - profile.buildingHeadroom());
        assertTrue(profile.safeMaximumY() > profile.safeMinimumY());
    }
}

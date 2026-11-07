package design.inherent.skyweavecompat.terrain.city;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CityConfigurationTest {
    @Test void acceptsStricterBurialAndRejectsMalformedOrAmbiguousPolicies() {
        assertEquals(.95, CityConfiguration.parse("{\"minimumBuriedFraction\":0.95}").minimumBuriedFraction());
        for (String json : new String[]{"{}", "{\"minimumBuriedFraction\":1.01}",
                "{\"minimumBuriedFraction\":-0.1}", "{\"minimumBuriedFraction\":\".9\"}",
                "{\"minimumBuriedFraction\":.9,\"extra\":true}",
                "{\"minimumBuriedFraction\":0.9,\"minimumBuriedFraction\":0.1}"})
            assertThrows(IllegalArgumentException.class, () -> CityConfiguration.parse(json));
    }
}

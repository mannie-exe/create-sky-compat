package design.inherent.skyweavecompat.configuration;

import com.google.gson.*;
import design.inherent.skyweavecompat.terrain.GenerationProfile;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Profile parsing is independent of optional generator classes and never applies runtime settings. */
public final class ProfileJson {
    private static final Gson JSON = new Gson();
    public static GenerationProfile generation(String text) {
        var object = JsonParser.parseString(text).getAsJsonObject();
        Set<String> fields = Arrays.stream(GenerationProfile.class.getRecordComponents())
                .map(component -> component.getName()).collect(Collectors.toSet());
        if (!object.keySet().equals(fields)) {
            throw new IllegalArgumentException("Generation profile must contain exactly these fields: " + fields);
        }
        for (var entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("Expected a number for " + entry.getKey());
            }
            if (!entry.getKey().equals("activation")) try { entry.getValue().getAsBigDecimal().intValueExact(); }
            catch (ArithmeticException invalid) { throw new IllegalArgumentException("Expected an integer for " + entry.getKey(), invalid); }
        }
        return JSON.fromJson(object, GenerationProfile.class);
    }
    private ProfileJson() {}
}

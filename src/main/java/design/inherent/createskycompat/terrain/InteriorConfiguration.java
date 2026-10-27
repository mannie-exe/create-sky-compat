package design.inherent.createskycompat.terrain;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class InteriorConfiguration {
    public static final String FILE = "create_sky_compat-interiors.json";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile InteriorProfile profile = InteriorProfile.INITIAL;
    private InteriorConfiguration() {}
    public static InteriorProfile profile() { return profile; }

    public static InteriorProfile parse(String text) {
        var object = JsonParser.parseString(text).getAsJsonObject();
        var fields = Arrays.stream(InteriorProfile.class.getRecordComponents())
                .map(component -> component.getName()).collect(Collectors.toSet());
        if (!object.keySet().equals(fields)) throw new IllegalArgumentException("Interior profile requires exactly: " + fields);
        for (var entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) throw new IllegalArgumentException("Invalid " + entry.getKey());
            var value = entry.getValue().getAsJsonPrimitive();
            if (entry.getKey().equals("nativeCarvers") ? !value.isBoolean() : !value.isNumber())
                throw new IllegalArgumentException("Invalid type for " + entry.getKey());
        }
        object.get("shellThickness").getAsBigDecimal().intValueExact();
        return JSON.fromJson(object, InteriorProfile.class);
    }

    public static void load(Path directory) {
        Path path = directory.resolve(FILE);
        try {
            if (!Files.exists(path)) Files.writeString(path, JSON.toJson(InteriorProfile.INITIAL) + "\n", StandardOpenOption.CREATE_NEW);
            profile = parse(Files.readString(path));
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Cannot load " + path + ": " + failure.getMessage(), failure);
        }
    }
}

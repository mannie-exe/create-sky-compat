package design.inherent.createskycompat.terrain;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class OceanConfiguration {
    public static final String FILE = "create_sky_compat-oceans.json";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile OceanProfile profile = OceanProfile.INITIAL;
    private OceanConfiguration() {}
    public static OceanProfile profile() { return profile; }
    public static OceanProfile parse(String text) {
        var object = JsonParser.parseString(text).getAsJsonObject();
        var fields = Arrays.stream(OceanProfile.class.getRecordComponents()).map(c -> c.getName()).collect(Collectors.toSet());
        if (!object.keySet().equals(fields)) throw new IllegalArgumentException("Ocean profile requires exactly: " + fields);
        for (var entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) throw new IllegalArgumentException("Expected scalar for " + entry.getKey());
            var value = entry.getValue().getAsJsonPrimitive();
            if (entry.getKey().equals("enabled")) {
                if (!value.isBoolean()) throw new IllegalArgumentException("Expected boolean for enabled");
            } else {
                if (!value.isNumber()) throw new IllegalArgumentException("Expected number for " + entry.getKey());
                if (!Set.of("basinChance", "coastalChance", "perimeterChance").contains(entry.getKey())) value.getAsBigDecimal().intValueExact();
            }
        }
        return JSON.fromJson(object, OceanProfile.class);
    }
    public static void load(Path directory) {
        var path = directory.resolve(FILE);
        try {
            if (!Files.exists(path)) Files.writeString(path, JSON.toJson(OceanProfile.INITIAL) + "\n", StandardOpenOption.CREATE_NEW);
            profile = parse(Files.readString(path));
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load " + path + ": " + error.getMessage(), error);
        }
    }
}

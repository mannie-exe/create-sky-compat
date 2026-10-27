package design.inherent.createskycompat.terrain;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class WaterConfiguration {
    public static final String FILE = "create_sky_compat-water.json";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile WaterProfile profile = WaterProfile.INITIAL;
    private WaterConfiguration() {}
    public static WaterProfile profile() { return profile; }
    public static WaterProfile parse(String text) {
        var object = JsonParser.parseString(text).getAsJsonObject();
        var fields = Arrays.stream(WaterProfile.class.getRecordComponents()).map(c -> c.getName()).collect(Collectors.toSet());
        if (!object.keySet().equals(fields)) throw new IllegalArgumentException("Water profile requires exactly: " + fields);
        for (var entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Expected number for " + entry.getKey());
            if (!Set.of("chance", "overflowChance", "outletChance").contains(entry.getKey()))
                entry.getValue().getAsBigDecimal().intValueExact();
        }
        return JSON.fromJson(object, WaterProfile.class);
    }
    public static void load(Path directory) {
        var path = directory.resolve(FILE);
        try {
            if (!Files.exists(path)) Files.writeString(path, JSON.toJson(WaterProfile.INITIAL) + "\n", StandardOpenOption.CREATE_NEW);
            profile = parse(Files.readString(path));
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load " + path + ": " + error.getMessage(), error);
        }
    }
}

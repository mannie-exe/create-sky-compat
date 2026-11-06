package design.inherent.skyweavecompat.configuration;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A typed file draft. Invalid input stays in the draft and cannot reach disk. */
public final class JsonEdits {
    public enum Kind { NUMBER, BOOLEAN, STRINGS }
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final ConfigFile file;
    private final JsonSettings.Definition definition;
    private final List<String> keys;
    private String baseline;
    private JsonObject saved;
    private final Map<String, String> edits = new LinkedHashMap<>();

    public JsonEdits(Path directory, JsonSettings.Definition definition) throws IOException {
        if (!JsonSettings.FILES.contains(definition)) throw new IllegalArgumentException("Unknown configuration");
        this.definition = definition;
        keys = List.copyOf(JsonParser.parseString(definition.defaults()).getAsJsonObject().keySet());
        file = new ConfigFile(directory, definition.filename());
        reload();
    }
    public JsonSettings.Definition definition() { return definition; }
    public List<String> keys() { return keys; }
    public Kind kind(String key) {
        JsonElement value = saved.get(key);
        if (value == null) throw new IllegalArgumentException("Unknown setting: " + key);
        if (value.isJsonArray()) return Kind.STRINGS;
        if (!value.isJsonPrimitive()) throw new IllegalArgumentException("Unsupported setting: " + key);
        return value.getAsJsonPrimitive().isBoolean() ? Kind.BOOLEAN : Kind.NUMBER;
    }
    private String display(JsonElement value) { return value.isJsonArray() ? value.toString() : value.getAsString(); }
    public String value(String key) { kind(key); return edits.getOrDefault(key, display(saved.get(key))); }
    public String defaultValue(String key) { return display(JsonParser.parseString(definition.defaults()).getAsJsonObject().get(key)); }
    public void set(String key, String value) {
        kind(key);
        if (value.equals(display(saved.get(key)))) edits.remove(key); else edits.put(key, value);
    }
    public void reset(String key) { set(key, defaultValue(key)); }
    public List<String> strings(String key) {
        if (kind(key) != Kind.STRINGS) throw new IllegalArgumentException("Not a string list");
        var array = JsonParser.parseString(value(key)).getAsJsonArray();
        return array.asList().stream().map(JsonElement::getAsString).toList();
    }
    public void strings(String key, List<String> values) { set(key, new Gson().toJson(values)); }
    public boolean dirty() { return !edits.isEmpty(); }
    public boolean edited(String key) { return edits.containsKey(key); }
    public void discard() { edits.clear(); }
    public void reload() throws IOException {
        String current = file.read();
        JsonObject parsed = object(current);
        definition.validate().accept(current);
        baseline = current; saved = parsed; edits.clear();
    }
    public String candidate() {
        JsonObject result = saved.deepCopy();
        edits.forEach((key, raw) -> {
            try {
                switch (kind(key)) {
                    case NUMBER -> result.add(key, new JsonPrimitive(new BigDecimal(raw.strip())));
                    case BOOLEAN -> {
                        if (!raw.equals("true") && !raw.equals("false")) throw new IllegalArgumentException("Expected true or false");
                        result.addProperty(key, Boolean.parseBoolean(raw));
                    }
                    case STRINGS -> {
                        var array = JsonParser.parseString(raw).getAsJsonArray();
                        for (var item : array) if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString())
                            throw new IllegalArgumentException("Expected map IDs");
                        if (array.size() != array.asList().stream().map(JsonElement::getAsString).distinct().count())
                            throw new IllegalArgumentException("Map IDs must be unique");
                        result.add(key, array);
                    }
                }
            } catch (RuntimeException invalid) { throw new IllegalArgumentException(key + ": " + invalid.getMessage(), invalid); }
        });
        String text = JSON.toJson(result) + "\n";
        definition.validate().accept(text);
        return baseline.contains("\r\n") ? text.replace("\n", "\r\n") : text;
    }
    public void check() throws IOException { candidate(); file.check(baseline); }
    public void save() throws IOException {
        if (!dirty()) return;
        String next = candidate();
        String actual = file.write(baseline, next);
        definition.validate().accept(actual);
        saved = object(actual); baseline = actual; edits.clear();
    }

    // Reject duplicate keys rather than letting Gson silently discard one value.
    static JsonObject object(String text) throws IOException {
        try (var reader = new JsonReader(new StringReader(text))) {
            var value = read(reader, 0);
            if (!value.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IOException("Expected one JSON object");
            return value.getAsJsonObject();
        }
    }
    private static JsonElement read(JsonReader reader, int depth) throws IOException {
        if (depth > 16) throw new IOException("JSON nesting exceeds configuration limit");
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                var object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw new IOException("Duplicate JSON key: " + name);
                    object.add(name, read(reader, depth + 1));
                }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                var array = new JsonArray(); reader.beginArray();
                while (reader.hasNext()) array.add(read(reader, depth + 1));
                reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IOException("Unexpected JSON token");
        };
    }
}

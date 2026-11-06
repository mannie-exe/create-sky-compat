package design.inherent.skyweavecompat.configuration;

import design.inherent.skyweavecompat.Feature;
import design.inherent.skyweavecompat.FeatureSettings;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** A next-start draft; never mutates the immutable settings selected by the loader. */
public final class StartupEdits {
    public static final class ChangedOnDisk extends IOException {
        public ChangedOnDisk() { super("The startup file changed outside this editor"); }
    }
    private final ConfigFile file;
    private String baseline;
    private FeatureSettings saved;
    private final Map<Feature, Boolean> edits = new EnumMap<>(Feature.class);

    private StartupEdits(Path directory) throws IOException { this.file = new ConfigFile(directory, FeatureSettings.FILE); reload(); }
    public static StartupEdits open(Path configDirectory) throws IOException {
        return new StartupEdits(configDirectory);
    }
    private String read() throws IOException { return file.read(); }
    public void check() throws IOException {
        try { file.check(baseline); } catch (ConfigFile.ChangedOnDisk conflict) { throw new ChangedOnDisk(); }
    }
    public void reload() throws IOException {
        String text = read();
        FeatureSettings parsed = FeatureSettings.read(new StringReader(text));
        baseline = text; saved = parsed; edits.clear();
    }
    public FeatureSettings saved() { return saved; }
    public boolean value(Feature feature) { return edits.getOrDefault(feature, saved.enabled(feature)); }
    public void set(Feature feature, boolean value) {
        if (value == saved.enabled(feature)) edits.remove(feature); else edits.put(feature, value);
    }
    public boolean dirty() { return !edits.isEmpty(); }
    public boolean edited(Feature feature) { return edits.containsKey(feature); }
    public void discard() { edits.clear(); }
    public Map<Feature, Boolean> changes() { return Map.copyOf(edits); }

    public void save() throws IOException {
        if (!dirty()) return;
        check();
        String candidate = replace(baseline, edits);
        FeatureSettings parsed = FeatureSettings.read(new StringReader(candidate));
        try { baseline = file.write(baseline, candidate); }
        catch (ConfigFile.ChangedOnDisk conflict) { throw new ChangedOnDisk(); }
        saved = parsed; edits.clear();
    }

    private static String replace(String original, Map<Feature, Boolean> edits) throws IOException {
        String newline = original.contains("\r\n") ? "\r\n" : "\n";
        var remaining = new EnumMap<Feature, Boolean>(Feature.class); remaining.putAll(edits);
        var result = new StringBuilder();
        for (String chunk : propertyChunks(original)) {
            var property = new Properties(); property.load(new StringReader(chunk));
            Feature changed = null;
            for (Feature feature : remaining.keySet()) if (property.containsKey(feature.key)) { changed = feature; break; }
            if (changed == null) result.append(chunk);
            else {
                String indent = chunk.substring(0, chunk.length() - chunk.stripLeading().length());
                result.append(indent).append(changed.key).append('=').append(remaining.remove(changed));
                if (chunk.endsWith("\n")) result.append(newline);
            }
        }
        for (Feature feature : Feature.values()) if (remaining.containsKey(feature)) {
            if (!result.isEmpty() && result.charAt(result.length() - 1) != '\n') result.append(newline);
            result.append(feature.key).append('=').append(remaining.get(feature)).append(newline);
        }
        return result.toString();
    }
    private static List<String> propertyChunks(String text) {
        var chunks = new ArrayList<String>(); var current = new StringBuilder(); boolean continued = false;
        for (String line : text.split("(?<=\n)")) {
            current.append(line);
            String stripped = line.replaceFirst("[\\r\\n]+$", "");
            boolean comment = !continued && (stripped.stripLeading().startsWith("#") || stripped.stripLeading().startsWith("!"));
            int slashes = 0;
            for (int i = stripped.length() - 1; i >= 0 && stripped.charAt(i) == '\\'; i--) slashes++;
            continued = !comment && slashes % 2 == 1;
            if (!continued) { chunks.add(current.toString()); current.setLength(0); }
        }
        if (!current.isEmpty()) chunks.add(current.toString());
        return chunks;
    }
}

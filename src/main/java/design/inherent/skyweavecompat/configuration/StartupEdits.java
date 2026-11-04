package design.inherent.skyweavecompat.configuration;

import design.inherent.skyweavecompat.Feature;
import design.inherent.skyweavecompat.FeatureSettings;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
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
    private final Path path;
    private String baseline;
    private FeatureSettings saved;
    private final Map<Feature, Boolean> edits = new EnumMap<>(Feature.class);

    private StartupEdits(Path path) throws IOException { this.path = path; reload(); }
    public static StartupEdits open(Path configDirectory) throws IOException {
        return new StartupEdits(configDirectory.resolve(FeatureSettings.FILE));
    }
    private String read() throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path))
            throw new IOException("Startup settings must be a regular local file");
        if (Files.size(path) > 131072) throw new IOException("Startup settings exceed 128 KiB");
        return Files.readString(path, StandardCharsets.UTF_8);
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
        if (!read().equals(baseline)) throw new ChangedOnDisk();
        if (!Files.isWritable(path)) throw new IOException("Startup settings are not writable");
        String candidate = replace(baseline, edits);
        FeatureSettings parsed = FeatureSettings.read(new StringReader(candidate));
        if (candidate.getBytes(StandardCharsets.UTF_8).length > 131072) throw new IOException("Startup settings exceed 128 KiB");
        Path temporary = Files.createTempFile(path.getParent(), ".compat-settings-", ".tmp");
        try {
            var attributes = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes != null) Files.setPosixFilePermissions(temporary, attributes.readAttributes().permissions());
            Files.writeString(temporary, candidate, StandardCharsets.UTF_8);
            if (!read().equals(baseline)) throw new ChangedOnDisk();
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            baseline = candidate; saved = parsed; edits.clear();
        } finally { Files.deleteIfExists(temporary); }
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

package design.inherent.skyweavecompat.shader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Bounded in-memory shader archives; extracted data never controls a filesystem path. */
public final class ShaderArchives {
    private ShaderArchives() {}
    public static final int ENTRY_LIMIT = 32 * 1024 * 1024;
    public static final int TOTAL_LIMIT = 256 * 1024 * 1024;

    public static String path(String name) throws IOException {
        if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.matches(".*[\\\\:*?\"<>|].*") || name.chars().anyMatch(c -> c < 32))
            throw new IOException("Unsafe archive path: " + name);
        for (String component : name.split("/", -1))
            if (component.equals(".") || component.equals("..") || component.isEmpty() || component.endsWith(" ") || component.endsWith(".") || component.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?"))
                throw new IOException("Unsafe archive path: " + name);
        return name;
    }

    public static Map<String, byte[]> read(byte[] bytes) throws IOException {
        var files = new TreeMap<String, byte[]>();
        int total = 0, count = 0;
        var members = new java.util.HashSet<String>();
        try (var input = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (++count > 12000) throw new IOException("Archive has too many entries");
                String name = path(entry.isDirectory() ? entry.getName().replaceFirst("/$", "") : entry.getName());
                if (!members.add(name.toLowerCase(java.util.Locale.ROOT))) throw new IOException("Duplicate archive entry: " + name);
                if (entry.isDirectory()) continue;
                byte[] data = input.readNBytes(ENTRY_LIMIT + 1);
                if (data.length > ENTRY_LIMIT || (total += data.length) > TOTAL_LIMIT)
                    throw new IOException("Archive exceeds expansion limit");
                if (files.putIfAbsent(name, data) != null) throw new IOException("Duplicate archive member: " + name);
            }
        }
        if (files.isEmpty()) throw new IOException("Expected a nonempty ZIP archive");
        validate(files);
        return files;
    }

    /** Stored entries avoid platform/JDK deflater differences in the output identity. */
    public static byte[] write(Map<String, byte[]> files) throws IOException {
        validate(files);
        var bytes = new ByteArrayOutputStream();
        try (var output = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (var file : new TreeMap<>(files).entrySet()) {
                var entry = new ZipEntry(path(file.getKey()));
                byte[] data = file.getValue();
                var crc = new CRC32(); crc.update(data);
                entry.setMethod(ZipEntry.STORED); entry.setSize(data.length); entry.setCompressedSize(data.length);
                entry.setCrc(crc.getValue()); entry.setTimeLocal(java.time.LocalDateTime.of(1980, 1, 1, 0, 0));
                output.putNextEntry(entry); output.write(data); output.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static void validate(Map<String, byte[]> files) throws IOException {
        if (files.size() > 12000) throw new IOException("Too many archive files");
        var names = new java.util.HashSet<String>(); long total = 0;
        for (var file : files.entrySet()) {
            String name = path(file.getKey());
            if (!names.add(name.toLowerCase(java.util.Locale.ROOT))) throw new IOException("Case-colliding archive paths");
            if (file.getValue().length > ENTRY_LIMIT || (total += file.getValue().length) > TOTAL_LIMIT - 4 * 1024 * 1024) throw new IOException("Archive exceeds limit");
        }
        for (String name : names) {
            for (int at = name.indexOf('/'); at >= 0; at = name.indexOf('/', at + 1))
                if (names.contains(name.substring(0, at))) throw new IOException("Archive file/directory collision");
        }
    }

    public static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}

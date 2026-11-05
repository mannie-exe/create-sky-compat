package design.inherent.skyweavecompat.shader;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Adapter for the exact reviewed shaded BSDIFF engine, never a recipe-selected plugin. */
final class EuphoriaPatch {
    private static final String PIN = "fe64a1fad6b6f39a95f22ea6ba9506b712e36b96c46b80f1cf38fe144900a4b2";
    private static final String SHADE = "com.euphoriapatches.shadow.";
    private EuphoriaPatch() {}
    static Map<String, byte[]> apply(Map<String, byte[]> files, byte[] original, byte[] patcher, Path work) throws Exception {
        if (!ShaderArchives.sha256(patcher).equals(PIN)) throw new IOException("Unreviewed Euphoria patch engine");
        var source = new TreeMap<>(files);
        String common = "shaders/lib/common.glsl";
        source.put(common, style(source.get(common), "1"));
        Files.createDirectories(work);
        Path temporary = Files.createTempDirectory(work, "euphoria-");
        try {
            Path jar = temporary.resolve("engine.jar"), base = temporary.resolve("base.tar"), result = temporary.resolve("result.tar"), delta = temporary.resolve("euphoria.patch");
            Files.write(jar, patcher);
            Files.write(delta, ShaderEngine.member(patcher, "EuphoriaPatches_1.10.5.patch"));
            try (var loader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                var entryType = loader.loadClass(SHADE + "org.apache.commons.compress.archivers.tar.TarArchiveEntry");
                var streamType = loader.loadClass(SHADE + "org.apache.commons.compress.archivers.tar.TarArchiveOutputStream");
                var archiveType = loader.loadClass(SHADE + "org.apache.commons.compress.archivers.ArchiveEntry");
                var directories = new TreeSet<String>(); directories.add("");
                try (var zip = new ZipInputStream(new ByteArrayInputStream(original))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (entry.isDirectory()) directories.add(ShaderArchives.path(entry.getName().replaceFirst("/$", "")));
                    }
                }
                for (String name : source.keySet()) {
                    int at = name.lastIndexOf('/');
                    while (at >= 0) { directories.add(name.substring(0, at)); at = name.lastIndexOf('/', at - 1); }
                }
                var names = new TreeSet<>(directories); names.addAll(source.keySet());
                try (var bytes = Files.newOutputStream(base); var tar = (OutputStream) streamType.getConstructor(OutputStream.class).newInstance(bytes)) {
                    for (String name : names) {
                        boolean directory = directories.contains(name);
                        // The upstream normalization uses File entries, including the root directory.
                        Path path = temporary.resolve("tree").resolve(name);
                        if (directory) Files.createDirectories(path);
                        else { Files.createDirectories(path.getParent()); Files.write(path, source.get(name)); }
                        Object entry = entryType.getConstructor(File.class, String.class).newInstance(path.toFile(), name);
                        entryType.getMethod("setModTime", long.class).invoke(entry, 0L);
                        entryType.getMethod("setSize", long.class).invoke(entry, directory ? 0L : (long) source.get(name).length);
                        entryType.getMethod("setIds", int.class, int.class).invoke(entry, 0, 0);
                        entryType.getMethod("setNames", String.class, String.class).invoke(entry, "", "");
                        entryType.getMethod("setMode", int.class).invoke(entry, 0100644);
                        streamType.getMethod("putArchiveEntry", archiveType).invoke(tar, entry);
                        if (!directory) tar.write(source.get(name));
                        streamType.getMethod("closeArchiveEntry").invoke(tar);
                    }
                    streamType.getMethod("finish").invoke(tar);
                }
                if (Files.size(base) != 1651200) throw new IOException("Changed normalized Complementary TAR");
                loader.loadClass(SHADE + "io.sigpipe.jbsdiff.ui.FileUI").getMethod("patch", File.class, File.class, File.class)
                    .invoke(null, base.toFile(), result.toFile(), delta.toFile());
                var patched = readTar(ShaderEngine.bounded(result, ShaderArchives.TOTAL_LIMIT));
                patched.put(common, style(patched.get(common), "4"));
                return patched;
            }
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
    private static byte[] style(byte[] bytes, String value) throws IOException {
        if (bytes == null) throw new IOException("Missing shader style");
        String text = new String(bytes, StandardCharsets.UTF_8);
        var matcher = java.util.regex.Pattern.compile("SHADER_STYLE [14]").matcher(text);
        if (!matcher.find()) throw new IOException("Changed shader style declaration");
        return matcher.replaceFirst("SHADER_STYLE " + value).getBytes(StandardCharsets.UTF_8);
    }
    private static Map<String, byte[]> readTar(byte[] bytes) throws IOException {
        var result = new TreeMap<String, byte[]>(); int total = 0;
        for (int at = 0, count = 0; at + 512 <= bytes.length; ) {
            if (bytes[at] == 0) break;
            if (++count > 12000) throw new IOException("Too many TAR entries");
            String name = new String(bytes, at, 100, StandardCharsets.UTF_8).split("\\x00", 2)[0];
            String sizeText = new String(bytes, at + 124, 12, StandardCharsets.US_ASCII).replace("\0", "").trim();
            int size;
            try { size = Integer.parseInt(sizeText, 8); } catch (NumberFormatException e) { throw new IOException("Invalid TAR size", e); }
            byte type = bytes[at + 156];
            if (size < 0 || size > ShaderArchives.ENTRY_LIMIT || (total += size) > ShaderArchives.TOTAL_LIMIT || at + 512L + size > bytes.length)
                throw new IOException("Oversized/truncated TAR");
            if (type == '0' || type == 0) {
                if (name.startsWith("./")) name = name.substring(2);
                ShaderArchives.path(name);
                if (result.putIfAbsent(name, Arrays.copyOfRange(bytes, at + 512, at + 512 + size)) != null) throw new IOException("Duplicate TAR entry");
            } else if (type != '5') throw new IOException("Unsupported TAR entry type");
            at += 512 + ((size + 511) / 512) * 512;
        }
        if (result.isEmpty()) throw new IOException("Empty patched TAR");
        return result;
    }
}

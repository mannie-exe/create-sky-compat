package design.inherent.createskycompat.dh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/** Save-instance identity, independent of seed, generation settings and connection lifetime. */
public final class WorldIdentity {
    public static final String FILE = "create-sky-lod-world-id";
    private WorldIdentity() {}

    /** Null preserves DH's existing key until the operator opts in; existing markers remain authoritative. */
    public static String selectKey(Path root, boolean enabled, String dimension) throws IOException {
        if (!enabled && !Files.exists(root.resolve(FILE), LinkOption.NOFOLLOW_LINKS)) return null;
        return dhLevelKey(loadOrCreate(root), dimension);
    }

    public static synchronized UUID loadOrCreate(Path worldRoot) throws IOException {
        if (!Files.isDirectory(worldRoot, LinkOption.NOFOLLOW_LINKS)) throw new IOException("World identity requires an existing save directory");
        var file = worldRoot.resolve(FILE);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] encoded = (UUID.randomUUID() + "\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            try (var channel = java.nio.channels.FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            } catch (java.nio.file.FileAlreadyExistsException race) { /* Another caller created this save's identity. */ }
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) != 37)
            throw new IOException("Invalid saved LOD world identity");
        String value = Files.readString(file);
        try {
            var id = UUID.fromString(value.strip());
            if (!(id + "\n").equals(value)) throw new IOException("Noncanonical saved LOD world identity");
            return id;
        } catch (IllegalArgumentException failure) { throw new IOException("Invalid saved LOD world identity", failure); }
    }

    /** Stable DH namespace independent of seed and session. */
    public static String dhLevelKey(UUID world, String dimension) {
        java.util.Objects.requireNonNull(world);
        if (dimension == null || !dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid dimension identifier");
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(dimension.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "csky_" + world.toString().replace("-", "") + "@dim_" + java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    public static boolean isDhLevelKey(String key) {
        return key != null && key.matches("csky_[0-9a-f]{32}@dim_[0-9a-f]{64}");
    }
}

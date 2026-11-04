package design.inherent.skyweavecompat.terrain;

import java.io.IOException;
import java.nio.file.*;

/** Fail before generation rather than silently joining two incompatible layouts. */
public final class GenerationIdentity {
    public static final String FILE = "skyweave-generation.json";
    private GenerationIdentity() {}

    /** Old registry IDs cannot be converted by renaming a marker or disabling terrain. */
    public static void rejectLegacy(Path world) {
        if (Files.exists(world.resolve("create-sky-generation.json"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("This world uses Create Sky Compat registry identities. "
                    + "Open it with its original release; Skyweave Compat requires a separate fresh world.");
        }
    }

    public static void verify(Path world, String identity) throws IOException {
        rejectLegacy(world);
        Path marker = world.resolve(FILE);
        if (Files.exists(marker)) {
            if (!Files.readString(marker).equals(identity)) {
                throw new IllegalStateException("Skyweave generation settings differ from this world's recorded identity. "
                        + "Restore its original mod/settings or create a separate world; automatic conversion is unsupported.");
            }
            return;
        }
        Path region = world.resolve("region");
        if (Files.isDirectory(region)) try (var files = Files.list(region)) {
            if (files.anyMatch(p -> p.getFileName().toString().endsWith(".mca"))) {
                throw new IllegalStateException("Existing terrain has no Skyweave generation identity; refusing automatic conversion.");
            }
        }
        Files.createDirectories(world);
        // CREATE_NEW protects an unexpected concurrent writer. A partial write fails closed on reopening.
        Files.writeString(marker, identity, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
}

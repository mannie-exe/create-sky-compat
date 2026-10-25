package design.inherent.createskycompat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Retain operator settings without overwriting a newly authored configuration. */
final class LegacyConfiguration {
    static final String OLD_NAME = "sky_village_neighbors-common.toml";
    static final String NEW_NAME = "create_sky_compat-common.toml";

    private LegacyConfiguration() {}

    static void migrate(Path directory) {
        Path oldFile = directory.resolve(OLD_NAME);
        Path newFile = directory.resolve(NEW_NAME);
        if (Files.exists(newFile, LinkOption.NOFOLLOW_LINKS)
                || !Files.isRegularFile(oldFile, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            Files.copy(oldFile, newFile);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot preserve legacy village settings in " + newFile, failure);
        }
    }
}

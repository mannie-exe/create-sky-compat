package design.inherent.createskycompat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LegacyConfigurationTest {
    @TempDir Path directory;

    @Test void preservesLegacyAndDoesNotOverwriteNewSettings() throws Exception {
        Path legacy = directory.resolve(LegacyConfiguration.OLD_NAME);
        Path current = directory.resolve(LegacyConfiguration.NEW_NAME);
        Files.writeString(legacy, "maximumGap = 384\n");
        LegacyConfiguration.migrate(directory);
        assertEquals(Files.readString(legacy), Files.readString(current));
        Files.writeString(current, "maximumGap = 768\n");
        LegacyConfiguration.migrate(directory);
        assertEquals("maximumGap = 768\n", Files.readString(current));
        assertEquals("maximumGap = 384\n", Files.readString(legacy));
    }

    @Test void freshInstallationDoesNotCreateAConfig() {
        LegacyConfiguration.migrate(directory);
        assertFalse(Files.exists(directory.resolve(LegacyConfiguration.NEW_NAME)));
    }
}

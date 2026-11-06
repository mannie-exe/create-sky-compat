package design.inherent.skyweavecompat.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ConfigFileTest {
    @TempDir Path directory;

    @Test void registeredOwnerIsNotCalledForStaleDataAndReadbackFailuresPropagate() throws Exception {
        Path path = directory.resolve("native.toml"); Files.writeString(path, "value=1\n");
        var file = new ConfigFile(directory, "native.toml"); String baseline = file.read();
        Files.writeString(path, "value=2\n"); var called = new AtomicBoolean();
        assertThrows(ConfigFile.ChangedOnDisk.class, () -> file.throughOwner(baseline, () -> called.set(true)));
        assertFalse(called.get()); assertEquals("value=2\n", Files.readString(path));
        String current = file.read();
        assertThrows(IOException.class, () -> file.throughOwner(current, () -> Files.delete(path)));
    }

    @Test void failedTemporaryFileCreationLeavesTheOriginalAndNoResidue() throws Exception {
        Path path = directory.resolve("owned.json"); Files.writeString(path, "{}\n");
        var file = new ConfigFile(directory, "owned.json");
        var original = Files.getPosixFilePermissions(directory);
        try {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("r-x------"));
            assumeFalse(Files.isWritable(directory), "Privileged host bypasses mode permissions");
            assertThrows(IOException.class, () -> file.write("{}\n", "{\"value\":1}\n"));
            assertEquals("{}\n", Files.readString(path));
            try (var children = Files.list(directory)) { assertEquals(1, children.count()); }
        } finally { Files.setPosixFilePermissions(directory, original); }
    }
}

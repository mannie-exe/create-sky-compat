package design.inherent.createskycompat.terrain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class GenerationIdentityTest {
    @TempDir Path world;

    @Test void unchangedWorldReopensButNewSettingsCannotOverwriteIdentity() throws Exception {
        GenerationIdentity.verify(world, "first");
        GenerationIdentity.verify(world, "first");
        assertThrows(IllegalStateException.class, () -> GenerationIdentity.verify(world, "changed"));
        assertEquals("first", Files.readString(world.resolve(GenerationIdentity.FILE)));
    }

    @Test void populatedUnmarkedWorldIsNeverAdopted() throws Exception {
        Files.createDirectory(world.resolve("region"));
        Files.write(world.resolve("region/r.0.0.mca"), new byte[]{1});
        assertThrows(IllegalStateException.class, () -> GenerationIdentity.verify(world, "candidate"));
        assertFalse(Files.exists(world.resolve(GenerationIdentity.FILE)));
    }
}

package design.inherent.skyweavecompat.configuration;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RegisteredEditsTest {
    @TempDir Path directory;

    @Test void integerDraftsAndCrossFieldBoundsUseTheRegisteredSpecAndRuntimeRule() throws Exception {
        var builder = new ModConfigSpec.Builder();
        builder.worldRestart().defineInRange("minimumGap", 64, 0, 2048);
        builder.worldRestart().defineInRange("maximumGap", 512, 0, 2048);
        builder.worldRestart().defineInRange("minimumIslandRadius", 96, 0, 2048);
        var spec = builder.build(); Path file = write("skyweave_compat-common.toml", spec);
        var edits = new RegisteredEdits(directory, file.getFileName().toString(), spec);
        String original = Files.readString(file);
        edits.set("minimumGap", "600"); assertThrows(Exception.class, edits::candidate); assertEquals(original, Files.readString(file));
        edits.set("maximumGap", "700"); assertEquals(600, edits.candidate().get("minimumGap"));
        edits.set("minimumGap", "12.5"); assertThrows(Exception.class, edits::save); assertEquals("12.5", edits.value("minimumGap"));
        edits.reset("minimumGap"); assertEquals("64", edits.value("minimumGap"));
        edits.set("minimumGap", "-1"); assertThrows(Exception.class, edits::candidate);
    }

    enum Mode { FIRST, SECOND }
    @Test void enumChoicesAndExternalFileConflictsRetainTheDraft() throws Exception {
        var builder = new ModConfigSpec.Builder(); builder.defineEnum("mode", Mode.FIRST); var spec = builder.build();
        Path file = write("createredstonelinkgui-client.toml", spec);
        var edits = new RegisteredEdits(directory, file.getFileName().toString(), spec);
        assertEquals(java.util.List.of("FIRST", "SECOND"), edits.fields().getFirst().choices());
        edits.set("mode", "SECOND"); assertEquals(Mode.SECOND, edits.candidate().get("mode"));
        edits.set("mode", "INVALID"); assertThrows(Exception.class, edits::candidate);
        edits.set("mode", "SECOND"); Files.writeString(file, Files.readString(file) + "\n# outside editor\n");
        assertThrows(ConfigFile.ChangedOnDisk.class, edits::save); assertTrue(edits.dirty());
        assertTrue(Files.readString(file).contains("outside editor"));
        edits.reload(); assertFalse(edits.dirty()); assertEquals("FIRST", edits.value("mode"));
    }

    private Path write(String name, ModConfigSpec spec) throws Exception {
        var values = CommentedConfig.inMemory(); spec.correct(values);
        Path file = directory.resolve(name); Files.writeString(file, new TomlWriter().writeToString(values)); return file;
    }
}

package design.inherent.createskycompat.configuration;

import design.inherent.createskycompat.Feature;
import design.inherent.createskycompat.FeatureSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StartupEditsTest {
    @TempDir Path directory;
    private Path file(String source) throws IOException {
        Path path = directory.resolve(FeatureSettings.FILE); Files.writeString(path, source); return path;
    }
    @Test void editsOnlyChangedPropertiesAndRetainsCommentsOrderAndOmissions() throws IOException {
        Path path = file("# retain me\r\nquestlog.interface : true\r\n! retained too\r\nterrain.preset=false");
        var draft = StartupEdits.open(directory);
        draft.set(Feature.QUESTLOG_INTERFACE, false);
        draft.set(Feature.DH_CACHE, true);
        assertTrue(draft.dirty()); assertTrue(draft.saved().enabled(Feature.QUESTLOG_INTERFACE));
        draft.save();
        assertEquals("# retain me\r\nquestlog.interface=false\r\n! retained too\r\nterrain.preset=false\r\ndistant_horizons.world_cache=true\r\n", Files.readString(path));
        assertFalse(draft.dirty()); assertFalse(draft.saved().enabled(Feature.QUESTLOG_INTERFACE));
        assertFalse(draft.saved().values().containsKey(Feature.SABLE_GRAVES));
    }
    @Test void escapedKeysAndContinuedValuesUseTheExistingPropertySemantics() throws IOException {
        Path path = file("# comment ends with a slash \\\nquestlog\\.interface=tr\\\n ue\n\nterrain.preset=true\n");
        var draft = StartupEdits.open(directory);draft.set(Feature.QUESTLOG_INTERFACE, false);draft.save();
        assertEquals("# comment ends with a slash \\\nquestlog.interface=false\n\nterrain.preset=true\n",Files.readString(path));
    }
    @Test void changingBackToSavedValueAndDiscardDoNotWriteOrMaterializeDefaults() throws IOException {
        Path path = file("# defaults only\n"); var draft = StartupEdits.open(directory);
        draft.set(Feature.QUESTLOG_INTERFACE, false);draft.set(Feature.QUESTLOG_INTERFACE, true);
        assertFalse(draft.dirty());draft.save();assertEquals("# defaults only\n",Files.readString(path));
        draft.set(Feature.DH_CACHE,true);draft.discard();assertFalse(draft.value(Feature.DH_CACHE));
    }
    @Test void externalEditRefusesOverwriteAndKeepsDraftUntilExplicitReload() throws IOException {
        Path path = file("questlog.interface=true\n");var draft=StartupEdits.open(directory);
        draft.set(Feature.QUESTLOG_INTERFACE,false);Files.writeString(path,"# another editor\nquestlog.interface=true\n");
        assertThrows(StartupEdits.ChangedOnDisk.class,draft::save);assertTrue(draft.dirty());
        assertEquals("# another editor\nquestlog.interface=true\n",Files.readString(path));
        draft.reload();assertFalse(draft.dirty());assertTrue(draft.value(Feature.QUESTLOG_INTERFACE));
    }
    @Test void invalidFilesRemainUntouchedAndDoNotReplacePreviousDraftOnReload() throws IOException {
        Path path=file("questlog.interface=true\n");var draft=StartupEdits.open(directory);draft.set(Feature.QUESTLOG_INTERFACE,false);
        String broken="questlog.interface=no\n";Files.writeString(path,broken);
        assertThrows(IllegalArgumentException.class,draft::reload);assertTrue(draft.dirty());assertFalse(draft.value(Feature.QUESTLOG_INTERFACE));
        assertThrows(StartupEdits.ChangedOnDisk.class,draft::save);assertEquals(broken,Files.readString(path));
        Files.writeString(path,"unknown.key=true\n");assertThrows(IllegalArgumentException.class,()->StartupEdits.open(directory));
    }
    @Test void removedFileDoesNotBecomeANewDefaultsFile() throws IOException {
        Path path=file("");var draft=StartupEdits.open(directory);draft.set(Feature.TERRAIN,false);Files.delete(path);
        assertThrows(IOException.class,draft::save);assertTrue(draft.dirty());assertFalse(Files.exists(path));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
    @Test void readOnlyFileAndSymlinkCannotBeReplaced() throws IOException {
        Path path=file("questlog.interface=true\n");var draft=StartupEdits.open(directory);draft.set(Feature.QUESTLOG_INTERFACE,false);
        if (Files.getFileStore(directory).supportsFileAttributeView("posix")) {
            var permissions=Files.getPosixFilePermissions(path);
            try {
                Files.setPosixFilePermissions(path,Set.of(PosixFilePermission.OWNER_READ));
                assertThrows(IOException.class,draft::save);assertTrue(draft.dirty());
            } finally {Files.setPosixFilePermissions(path,permissions);}
            Path outside=directory.resolve("outside.properties");Files.move(path,outside);Files.createSymbolicLink(path,outside);
            assertThrows(IOException.class,draft::save);assertEquals("questlog.interface=true\n",Files.readString(outside));
        }
    }
}

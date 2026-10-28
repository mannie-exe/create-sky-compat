package design.inherent.createskycompat.dh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorldIdentityTest {
    @TempDir Path temporary;

    @Test void disabledDefaultPreservesExistingKeysAndOptInSurvivesConfigReset() throws Exception {
        assertNull(WorldIdentity.selectKey(temporary, false, "minecraft:overworld"));
        assertFalse(Files.exists(temporary.resolve(WorldIdentity.FILE)));
        var key = WorldIdentity.selectKey(temporary, true, "minecraft:overworld");
        assertTrue(WorldIdentity.isDhLevelKey(key));
        assertEquals(key, WorldIdentity.selectKey(temporary, false, "minecraft:overworld"));
        Files.writeString(temporary.resolve(WorldIdentity.FILE), "broken");
        assertThrows(IOException.class, () -> WorldIdentity.selectKey(temporary, false, "minecraft:overworld"));
    }

    @Test void separatesNewSavesAndPreservesReopenedIdentity() throws Exception {
        var first = Files.createDirectory(temporary.resolve("first"));
        var second = Files.createDirectory(temporary.resolve("second"));
        var identity = WorldIdentity.loadOrCreate(first);
        assertEquals(identity, WorldIdentity.loadOrCreate(first));
        assertNotEquals(identity, WorldIdentity.loadOrCreate(second));
    }

    @Test void refusesCorruptAndLinkedMarkersWithoutReplacingThem() throws Exception {
        var root = Files.createDirectory(temporary.resolve("world"));
        var marker = root.resolve(WorldIdentity.FILE);
        Files.writeString(marker, "invalid");
        assertThrows(IOException.class, () -> WorldIdentity.loadOrCreate(root));
        assertEquals("invalid", Files.readString(marker));
        Files.delete(marker);
        var outside = temporary.resolve("outside"); Files.writeString(outside, "unchanged");
        Files.createSymbolicLink(marker, outside);
        assertThrows(IOException.class, () -> WorldIdentity.loadOrCreate(root));
        assertEquals("unchanged", Files.readString(outside));
    }

    @Test void concurrentFirstAccessKeepsOneCompleteIdentity() throws Exception {
        var root = Files.createDirectory(temporary.resolve("concurrent"));
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var results = new java.util.ArrayList<java.util.concurrent.Future<java.util.UUID>>();
            for (int i = 0; i < 32; i++) results.add(executor.submit(() -> {
                start.await(); return WorldIdentity.loadOrCreate(root);
            }));
            start.countDown();
            var expected = results.getFirst().get();
            for (var result : results) assertEquals(expected, result.get());
            assertEquals(expected + "\n", Files.readString(root.resolve(WorldIdentity.FILE)));
        }
    }

    @Test void cacheKeysSeparateSavesAndDimensionsWithoutPathSanitizationCollisions() throws Exception {
        var identity = WorldIdentity.loadOrCreate(temporary);
        var key = WorldIdentity.dhLevelKey(identity, "minecraft:overworld");
        assertEquals(key, WorldIdentity.dhLevelKey(WorldIdentity.loadOrCreate(temporary), "minecraft:overworld"));
        assertNotEquals(key, WorldIdentity.dhLevelKey(java.util.UUID.randomUUID(), "minecraft:overworld"));
        assertNotEquals(key, WorldIdentity.dhLevelKey(identity, "minecraft:the_nether"));
        assertNotEquals(WorldIdentity.dhLevelKey(identity, "probe:a/b"),
                WorldIdentity.dhLevelKey(identity, "probe:a_b"));
        assertTrue(WorldIdentity.isDhLevelKey(key));
        assertTrue(WorldIdentity.isDhLevelKey(WorldIdentity.dhLevelKey(identity, "probe:" + "long/".repeat(100) + "dimension")));
        for (var other : new String[] {"minecraft:overworld", "ordinary@minecraft:overworld", key + "/", key.substring(1), "csky_foo@dim_bar"})
            assertFalse(WorldIdentity.isDhLevelKey(other));
        assertFalse(WorldIdentity.isDhLevelKey(null));
        assertThrows(IllegalArgumentException.class, () -> WorldIdentity.dhLevelKey(identity, "../outside"));
    }
}

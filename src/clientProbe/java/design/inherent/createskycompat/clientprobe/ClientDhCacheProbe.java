package design.inherent.createskycompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.seibel.distanthorizons.core.file.structure.ISaveStructure;
import com.seibel.distanthorizons.core.level.DhClientLevel;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import design.inherent.createskycompat.dh.WorldIdentity;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Executes the transformed client constructor's folder migration, stopping before renderer allocation. */
final class ClientDhCacheProbe {
    private boolean done;
    ClientDhCacheProbe() { NeoForge.EVENT_BUS.addListener(this::tick); }

    private void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (done || client.getOverlay() != null || !(client.screen instanceof TitleScreen)) return;
        done = true;
        var report = new JsonObject();
        try {
            var root = Files.createTempDirectory("create-sky-client-cache-");
            try {
                Class.forName("com.seibel.distanthorizons.core.generation.queues.RemoteWorldRetrievalQueue");
                verify(root.resolve("compat"), WorldIdentity.dhLevelKey(UUID.randomUUID(), "minecraft:overworld"), true);
                verify(root.resolve("foreign"), "ordinary@minecraft:overworld", false);
                verify(root.resolve("similar"), "csky_foo@dim_bar", false);
            } finally {
                try (var paths = Files.walk(root)) {
                    for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            }
            report.addProperty("compatKeyPreservedLegacyFolder", true);
            report.addProperty("ordinaryAndSimilarKeysKeptNativeMigration", true);
            report.addProperty("physicalClientConstructorExercised", true);
            report.addProperty("tcpOrTerrainRenderingExercised", false);
            report.addProperty("complete", true);
        } catch (Throwable failure) {
            report.addProperty("complete", false);
            report.addProperty("failure", failure.toString());
            org.slf4j.LoggerFactory.getLogger(getClass()).error("LOD client cache probe failed", failure);
        }
        try {
            Files.writeString(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("lod-cache-client-probe.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new IllegalStateException("Cannot save client probe result", failure); }
    }

    private static void verify(Path root, String key, boolean preserve) throws Exception {
        var old = Files.createDirectories(root.resolve("legacy"));
        Files.writeString(old.resolve("witness"), "existing terrain");
        var current = root.resolve("current");
        var saves = new ISaveStructure() {
            @Override public File getSaveFolder(ILevelWrapper level) { return current.toFile(); }
            @Override public File getPre23SaveFolder(ILevelWrapper level) { return old.toFile(); }
            @Override public void close() {}
        };
        var wrapper = (IServerKeyedClientLevel)Proxy.newProxyInstance(IServerKeyedClientLevel.class.getClassLoader(),
                new Class<?>[] {IServerKeyedClientLevel.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getServerLevelKey", "getDhIdentifier" -> key;
                    case "getServerKey" -> "probe-server";
                    case "setDhLevel" -> throw new MigrationFinished();
                    case "toString" -> key;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError("Unexpected migration fixture call: " + method);
                });
        try {
            new DhClientLevel(saves, wrapper, null, null);
            throw new AssertionError("Constructor did not reach the post-migration boundary");
        } catch (MigrationFinished expected) {
            if (!Files.isDirectory(current)) throw new AssertionError("Current cache directory was not created");
            var witness = (preserve ? old : current).resolve("witness");
            if (!Files.readString(witness).equals("existing terrain") || Files.exists(old) != preserve
                    || Files.exists(current.resolve("witness")) == preserve)
                throw new AssertionError("Wrong cache migration behavior for " + key);
        }
    }

    private static final class MigrationFinished extends RuntimeException {}
}

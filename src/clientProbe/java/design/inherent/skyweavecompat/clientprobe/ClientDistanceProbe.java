package design.inherent.skyweavecompat.clientprobe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRendererMode;
import com.seibel.distanthorizons.core.config.Config;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

/** Bounded renderer controls and observations in the marked resource fixture only. */
public final class ClientDistanceProbe {
    public static boolean captureNext;
    public static boolean captureLodNext;
    public static boolean captureActual;
    public static boolean nestedFailure;
    public static JsonObject actualDraw;
    public static JsonObject draw;
    public static JsonObject lodDraw;
    public static final class FixtureFailure extends RuntimeException {}
    private ClientDistanceProbe() {}

    static JsonObject observe(JsonObject command) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("skyweave_compat.probe.distance"))
            throw new IllegalStateException("Enable the isolated distance probe before using its controls");
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null)
            throw new IllegalStateException("Distance checks require the isolated integrated fixture");
        if (command.has("nestedFailure")) nestedFailure = command.get("nestedFailure").getAsBoolean();
        if (command.has("capture")) {
            captureNext = command.get("capture").getAsBoolean();
            captureLodNext = captureNext;
            captureActual = captureNext;
        }
        if (command.has("dh")) Config.Client.Advanced.Debugging.rendererMode.set(
                command.get("dh").getAsBoolean() ? EDhApiRendererMode.DEFAULT : EDhApiRendererMode.DISABLED);
        var config = Class.forName("net.ranold.ssrd.Config");
        if (command.has("chunks")) {
            int chunks = command.get("chunks").getAsInt();
            if (chunks < 1 || chunks > 256) throw new IllegalArgumentException("Fixture range is 1..256 chunks");
            config.getMethod("setPhysicsRenderDistance", int.class).invoke(null, chunks);
            var packet = Class.forName("net.ranold.ssrd.ClientConfigSyncPacket")
                    .getConstructor(int.class).newInstance(chunks);
            PacketDistributor.sendToServer((CustomPacketPayload) packet);
        }
        var row = new JsonObject();
        row.addProperty("dh", Config.Client.Advanced.Debugging.rendererMode.get().name());
        row.addProperty("range", config.getField("physicsRenderDistance").getInt(null));
        row.addProperty("nativeRange", mc.options.renderDistance().get());
        row.addProperty("depthFar", mc.gameRenderer.getDepthFar());
        if (draw != null) row.add("draw", draw.deepCopy());
        if (actualDraw != null) row.add("actualDraw", actualDraw.deepCopy());
        if (lodDraw != null) row.add("lodDraw", lodDraw.deepCopy());
        row.add("renderSystemProjection", matrix(RenderSystem.getProjectionMatrix()));
        var state = Class.forName("net.ranold.ssrd.SSRDState");
        for (String field : new String[]{"PURE_PROJ_MATRIX", "LEVEL_PROJ_MATRIX"}) {
            var value = state.getField(field).get(null);
            if (value != null) row.add(field, matrix((Matrix4f) value));
        }
        row.add("clientShips", ships(mc.level));
        var server = mc.getSingleplayerServer();
        var playerId = mc.player.getUUID();
        var serverReport = server.submit(() -> {
            try {
                var report = new JsonObject();
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Fixture player disconnected");
                report.add("ships", ships(player.serverLevel()));
                var requested = (Map<?, ?>) Class.forName("net.ranold.ssrd.ssrd").getField("playerRequestedRanges").get(null);
                if (requested.get(player) instanceof Number range) report.addProperty("range", range);
                return report;
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }).join();
        row.add("serverShips", serverReport.get("ships"));
        if (serverReport.has("range")) row.add("serverRequestedRange", serverReport.get("range"));
        return row;
    }

    private static JsonArray ships(net.minecraft.world.level.Level level) throws ReflectiveOperationException {
        var rows = new JsonArray();
        for (Object ship : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            var row = new JsonObject();
            row.addProperty("id", ship.getClass().getMethod("getUniqueId").invoke(ship).toString());
            if (level.isClientSide) {
                Object data = ship.getClass().getMethod("getRenderData").invoke(ship);
                row.addProperty("renderData", data.getClass().getName());
            }
            if (ship instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel serverShip) {
                var velocity = new JsonArray();
                for (double component : new double[]{serverShip.latestLinearVelocity.x, serverShip.latestLinearVelocity.y, serverShip.latestLinearVelocity.z}) velocity.add(component);
                row.add("linearVelocity", velocity);
                var spin = new JsonArray();
                for (double component : new double[]{serverShip.latestAngularVelocity.x, serverShip.latestAngularVelocity.y, serverShip.latestAngularVelocity.z}) spin.add(component);
                row.add("angularVelocity", spin);
                row.addProperty("physicsValid", dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(serverShip).isValid());
            }
            Object bounds = ship.getClass().getMethod("boundingBox").invoke(ship);
            for (String axis : new String[]{"minX", "minY", "minZ", "maxX", "maxY", "maxZ"})
                row.addProperty(axis, (Number) bounds.getClass().getMethod(axis).invoke(bounds));
            rows.add(row);
        }
        return rows;
    }

    private static JsonArray matrix(Matrix4f matrix) {
        var values = new JsonArray();
        for (float value : matrix.get(new float[16])) values.add(value);
        return values;
    }
}

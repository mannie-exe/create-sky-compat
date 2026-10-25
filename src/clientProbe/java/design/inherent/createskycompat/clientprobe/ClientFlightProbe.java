package design.inherent.createskycompat.clientprobe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3d;
import org.slf4j.LoggerFactory;

/** Optional read-only client telemetry. Never included in the released mod. */
@Mod(value = "create_sky_client_probe", dist = Dist.CLIENT)
public final class ClientFlightProbe {
    private int ticks;
    public ClientFlightProbe() { NeoForge.EVENT_BUS.addListener(this::tick); }
    private void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || ++ticks % 100 != 0) return;
        var level = client.level;
        var row = new JsonObject();
        row.addProperty("dimension", level.dimension().location().toString());
        row.addProperty("physics", DimensionPhysicsData.of(level).dimension().toString());
        row.addProperty("minY", level.getMinBuildHeight()); row.addProperty("maxY", level.getMaxBuildHeight());
        row.addProperty("playerY", client.player.getY());
        row.addProperty("rain", level.getRainLevel(1));
        var pressure = new JsonObject();
        for (int y : new int[]{-128, 180, 400, 508, 600, 680, 728, 768})
            pressure.addProperty(Integer.toString(y), DimensionPhysicsData.getAirPressure(level, new Vector3d(0, y, 0)));
        row.add("pressure", pressure);
        var ships = new JsonArray();
        for (Object ship : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            // Companion interfaces are nested upstream runtime artifacts, not compile dependencies.
            try {
                var item = new JsonObject();
                item.addProperty("id", ship.getClass().getMethod("getUniqueId").invoke(ship).toString());
                Object bounds = ship.getClass().getMethod("boundingBox").invoke(ship);
                item.addProperty("minY", (Number)bounds.getClass().getMethod("minY").invoke(bounds));
                item.addProperty("maxY", (Number)bounds.getClass().getMethod("maxY").invoke(bounds));
                ships.add(item);
            } catch (ReflectiveOperationException error) { throw new IllegalStateException("Flight fixture API changed", error); }
        }
        row.add("ships", ships);
        LoggerFactory.getLogger(ClientFlightProbe.class).info("CLIENT_FLIGHT_PROBE {}", row);
    }
}

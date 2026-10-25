package design.inherent.createskycompat.probe;

import com.google.gson.*;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysics;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import io.netty.buffer.Unpooled;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

/** Native Sable data loading, interpolation, dimension routing and wire codec. Flight is checked separately. */
final class AtmosphereProbe {
    private AtmosphereProbe() {}
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    static JsonObject run(ServerLevel level) {
        var physics = DimensionPhysicsData.of(level);
        check(physics.dimension().toString().equals("create_sky_compat:islands_v1"), "Scoped physics profile was not loaded/routed");
        double previous = Double.POSITIVE_INFINITY;
        for (int y = -128; y <= 768; y++) {
            double pressure = DimensionPhysicsData.getAirPressure(level, new Vector3d(0, y, 0));
            check(Double.isFinite(pressure) && pressure >= 0 && pressure <= 1.500001, "Invalid pressure at " + y);
            check(pressure <= previous + 1e-9, "Pressure increased with altitude at " + y);
            previous = pressure;
        }
        check(Math.abs(DimensionPhysicsData.getAirPressure(level, new Vector3d(0, 508, 0)) - 1) < 1e-9, "Inhabited pressure datum");
        check(DimensionPhysicsData.getAirPressure(level, new Vector3d(0, 768, 0)) == 0, "Build ceiling pressure");
        var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        check(!DimensionPhysicsData.of(nether).dimension().equals(physics.dimension()), "Physics leaked into Nether");
        var buffer = Unpooled.buffer();
        try {
            DimensionPhysics.STREAM_CODEC.encode(buffer, physics);
            var decoded = DimensionPhysics.STREAM_CODEC.decode(buffer);
            for (int y : new int[]{-128, 180, 508, 600, 680, 728, 768}) {
                check(decoded.pressureFunction().orElseThrow().evaluateFunction(y) == physics.pressureFunction().orElseThrow().evaluateFunction(y),
                        "Native wire codec changed pressure at " + y);
            }
        } finally { buffer.release(); }
        var rows = new JsonArray();
        for (int y : new int[]{-128, 180, 400, 508, 600, 680, 728, 768}) {
            var row = new JsonObject(); row.addProperty("y", y);
            row.addProperty("pressure", DimensionPhysicsData.getAirPressure(level, new Vector3d(0, y, 0))); rows.add(row);
        }
        var report = new JsonObject(); report.add("samples", rows); report.addProperty("monotonic", true);
        report.addProperty("wireRoundTrip", true); report.addProperty("netherUnchanged", true);
        return report;
    }
}

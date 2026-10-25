package design.inherent.createskycompat.probe;

import com.google.gson.JsonObject;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.util.Arrays;
import java.util.Set;

/** Installed force/sensor methods; controlled inputs do not prove a flyable aircraft. */
final class FlightMechanicsProbe {
    private FlightMechanicsProbe() {}

    static JsonObject inspect(ServerLevel level, SubLevel ship, BlockPos local, int worldY) throws Exception {
        var provider = Class.forName("dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider");
        var contextType = Class.forName(provider.getName() + "$LiftProviderContext");
        var groupType = Class.forName(provider.getName() + "$LiftProviderGroup");
        var sail = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:white_sail"));
        if (!provider.isInstance(sail)) throw new AssertionError("Installed sail is not a lift provider");
        var state = sail.defaultBlockState();
        var normal = (Direction)provider.getMethod("sable$getNormal", BlockState.class).invoke(sail, state);
        var direction = Vec3.atLowerCornerOf(normal.getNormal());
        var context = contextType.getConstructor(BlockPos.class, BlockState.class, Vec3.class).newInstance(local, state, direction);
        var method = Arrays.stream(provider.getMethods()).filter(m -> m.getName().equals("sable$contributeLiftAndDrag")).findFirst().orElseThrow();
        var result = new JsonObject();
        for (boolean moving : new boolean[]{false, true}) {
            var group = groupType.getConstructor(Set.class).newInstance(Set.of(local));
            var velocity = new Vector3d(direction.x, direction.y, direction.z).mul(-2);
            if (normal.getAxis() == Direction.Axis.X) velocity.y += 10; else velocity.x += 10;
            if (!moving) velocity.zero();
            var force = new Vector3d(); var torque = new Vector3d();
            method.invoke(sail, context, ship, null, 1.0, velocity, new Vector3d(), force, torque, group);
            double lift = groupType.getField("totalLiftStrength").getDouble(group);
            double drag = groupType.getField("totalDragStrength").getDouble(group);
            if (!Double.isFinite(lift) || !Double.isFinite(drag) || !force.isFinite() || !torque.isFinite())
                throw new AssertionError("Nonfinite aerodynamic output");
            if (moving ? lift <= 0 || drag <= 0 : lift != 0 || drag != 0)
                throw new AssertionError("Unexpected aerodynamic response at " + worldY + ": " + lift + "/" + drag);
            var row = new JsonObject(); row.addProperty("lift", lift); row.addProperty("drag", drag);
            row.addProperty("forceX", force.x); row.addProperty("forceY", force.y); row.addProperty("forceZ", force.z);
            result.add(moving ? "moving" : "stationary", row);
        }
        var sensorPos = new BlockPos(80, worldY, 80);
        var sensorBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("simulated:altitude_sensor"));
        level.setBlock(sensorPos, sensorBlock.defaultBlockState(), 2);
        var sensor = level.getBlockEntity(sensorPos);
        if (sensor == null) throw new AssertionError("Missing altitude sensor");
        var type = sensor.getClass();
        float height = ((Number)type.getMethod("getWorldHeight").invoke(sensor)).floatValue();
        float normalized = ((Number)type.getMethod("getNormalHeight").invoke(sensor)).floatValue();
        float restored = ((Number)type.getMethod("toWorldHeight", float.class).invoke(sensor, normalized)).floatValue();
        double pressure = ((Number)type.getMethod("getAirPressure").invoke(sensor)).doubleValue();
        double expected = DimensionPhysicsData.getAirPressure(level, new Vector3d(80.5, worldY + 0.5, 80.5));
        if (Math.abs(height - (worldY + 0.5)) > 0.001 || Math.abs(restored - height) > 0.001 || Math.abs(pressure - expected) > 1e-9)
            throw new AssertionError("Altitude sensor did not use expanded height/pressure");
        result.addProperty("sensorHeight", height); result.addProperty("sensorPressure", pressure);
        result.addProperty("sensorHeightRoundTrip", true);
        level.setBlock(sensorPos, Blocks.AIR.defaultBlockState(), 2);
        return result;
    }
}

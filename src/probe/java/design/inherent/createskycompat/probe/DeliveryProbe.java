package design.inherent.createskycompat.probe;

import com.google.gson.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.Arrays;

/** Exercise the actually merged delivery handler with the installed native surface method. */
final class DeliveryProbe {
    private DeliveryProbe() {}
    static JsonObject run(ServerLevel level, BlockPos locate, BoundingBox bounds) throws Exception {
        var type = Class.forName("com.ladderstar.adq.QuestGenerator");
        var nativeMethod = type.getDeclaredMethod("resolveDrySurface", ServerLevel.class, int.class, int.class);
        nativeMethod.setAccessible(true);
        var wrapper = Arrays.stream(type.getDeclaredMethods()).filter(method ->
                method.getName().contains("createSky$deliveryLanding") && method.getParameterCount() == 4
                        && method.getParameterTypes()[0] == ServerLevel.class).findFirst().orElseThrow();
        wrapper.setAccessible(true);
        Operation<BlockPos> nativeSurface = arguments -> {
            try { return (BlockPos)nativeMethod.invoke(null, arguments); }
            catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        };
        var original = nativeSurface.call(level, locate.getX(), locate.getZ());
        var actual = (BlockPos)wrapper.invoke(null, level, locate.getX(), locate.getZ(), nativeSurface);
        var row = new JsonObject(); row.add("locate", new Gson().toJsonTree(locate));
        row.addProperty("nativeAtLocateAccepted", original != null);
        row.addProperty("compatAccepted", actual != null);
        if (actual == null || !bounds.isInside(actual.below())) throw new AssertionError("No delivery surface inside the generated village");
        if (!actual.equals(nativeSurface.call(level, actual.getX(), actual.getZ()))) throw new AssertionError("Fallback bypassed native surface validation");
        if (original != null && !original.equals(actual)) throw new AssertionError("Changed a valid native endpoint");
        row.add("landing", new Gson().toJsonTree(actual));
        // A successful original result must not be retried, including in foreign dimensions.
        var nether = level.getServer().getLevel(Level.NETHER);
        int[] calls = {0};
        Operation<BlockPos> valid = arguments -> { calls[0]++; return new BlockPos(4, 80, 4); };
        if (!new BlockPos(4, 80, 4).equals(wrapper.invoke(null, nether, 4, 4, valid)) || calls[0] != 1)
            throw new AssertionError("Valid foreign endpoint changed");
        calls[0] = 0;
        Operation<BlockPos> absent = arguments -> { calls[0]++; return null; };
        if (wrapper.invoke(null, nether, 4, 4, absent) != null || calls[0] != 1)
            throw new AssertionError("Foreign endpoint retried");
        calls[0] = 0;
        if (wrapper.invoke(null, level, 1000000, 1000000, absent) != null || calls[0] != 1)
            throw new AssertionError("Unloaded/non-village endpoint retried");
        row.addProperty("scopeControls", true);
        var distance = Arrays.stream(type.getDeclaredMethods()).filter(method ->
                method.getName().contains("createSky$horizontalRouteDistance") && method.getParameterCount() == 4
                        && method.getParameterTypes()[3] == ServerLevel.class)
                .findFirst().orElseThrow();
        distance.setAccessible(true);
        Operation<Double> nativeDistance = arguments -> ((BlockPos)arguments[0]).distSqr((net.minecraft.core.Vec3i)arguments[1]);
        var unrealized = new BlockPos(2240, 0, -3887); var realized = new BlockPos(2240, 518, -3888);
        if (((Number)distance.invoke(null, unrealized, realized, nativeDistance, level)).doubleValue() != 1)
            throw new AssertionError("Locator height admitted a one-block delivery route");
        if (((Number)distance.invoke(null, unrealized, realized, nativeDistance, nether)).doubleValue() != unrealized.distSqr(realized))
            throw new AssertionError("Foreign route metric changed");
        if (((Number)distance.invoke(null, new BlockPos(0, 0, 0), new BlockPos(300, 518, 400), nativeDistance, level)).doubleValue() != 250000)
            throw new AssertionError("Horizontal minimum-distance boundary changed");
        row.addProperty("horizontalDistanceControls", true);
        return row;
    }
}

package design.inherent.createskycompat.probe;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.nio.file.*;

/** Calls the installed Aeronautics balloon search; this does not simulate aircraft flight. */
final class MechanicsProbe {
    private MechanicsProbe() {}
    static void run(MinecraftServer server, Path output) throws Exception {
        var level = server.overworld();
        var report = new JsonObject();
        report.addProperty("complete", false);
        report.add("atmosphere", AtmosphereProbe.run(level));
        var rows = new JsonArray(); report.add("balloons", rows);
        var gson = new GsonBuilder().setPrettyPrinting().create();
        // Reflection keeps optional instrumentation independent of an Aeronautics compile dependency.
        var builder = Class.forName("dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder");
        var graph = Class.forName("dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph");
        var build = builder.getMethod("buildBalloon", Level.class, BlockPos.class, graph);
        var pos = new BlockPos.MutableBlockPos();
        try {
            // A clear column reaches the real build ceiling, including from below vanilla's floor.
            for (int x = 64; x <= 70; x++) for (int z = 64; z <= 70; z++)
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++)
                    level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            for (int y : new int[]{-127, -96, 508, 760, 767}) {
                var row = new JsonObject(); row.addProperty("y", y); rows.add(row);
                long began = System.nanoTime();
                Object open = build.invoke(null, level, new BlockPos(67, y, 67), null);
                row.addProperty("openColumnRejected", open == null);
                row.addProperty("openMilliseconds", (System.nanoTime() - began) / 1e6);
                if (open != null) throw new AssertionError("Open atmosphere became a balloon at " + y);
                if (y + 4 >= level.getMaxBuildHeight()) continue;
                // Three by three enclosed air cavity, surrounded by native wool.
                for (int x = 65; x <= 69; x++) for (int z = 65; z <= 69; z++)
                    for (int by = y; by <= y + 4; by++) {
                        boolean shell = x == 65 || x == 69 || z == 65 || z == 69 || by == y || by == y + 4;
                        level.setBlock(pos.set(x, by, z), (shell ? Blocks.WHITE_WOOL : Blocks.AIR).defaultBlockState(), 2);
                    }
                var origin = new BlockPos(67, y + 1, 67);
                Object enclosed = build.invoke(null, level, origin, null);
                row.addProperty("enclosedDetected", enclosed != null);
                if (enclosed == null) throw new AssertionError("Enclosed wool cavity was not found at " + y);
                int volume = 0;
                var contains = graph.getMethod("hasBlockAt", BlockPos.class);
                for (int x = 66; x <= 68; x++) for (int z = 66; z <= 68; z++)
                    for (int by = y + 1; by <= y + 3; by++)
                        if ((boolean)contains.invoke(enclosed, new BlockPos(x, by, z))) volume++;
                row.addProperty("containedAirBlocks", volume);
                if (volume != 27) throw new AssertionError("Balloon cavity volume changed at " + y + ": " + volume);
                for (int x = 65; x <= 69; x++) for (int z = 65; z <= 69; z++)
                    for (int by = y; by <= y + 4; by++) level.setBlock(pos.set(x, by, z), Blocks.AIR.defaultBlockState(), 2);
                System.out.println("MECHANICS_BALLOON " + row);
            }
            report.add("assembly", assemblies(level));
            report.addProperty("complete", true);
        } finally { Files.writeString(output, gson.toJson(report) + "\n"); }
    }

    private static JsonArray assemblies(net.minecraft.server.level.ServerLevel level) throws Exception {
        var helper = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
        var assemble = helper.getMethod("assembleFromSingleBlock", Level.class, BlockPos.class, BlockPos.class,
                boolean.class, boolean.class);
        var disassemble = helper.getMethod("disassembleSubLevel", Level.class, SubLevel.class,
                BlockPos.class, BlockPos.class, Rotation.class, boolean.class);
        var rows = new JsonArray();
        double previousLift = Double.POSITIVE_INFINITY;
        for (int y : new int[]{-127, -96, 508, 766}) {
            var origin = new BlockPos(67, y, 67);
            level.setBlock(origin, Blocks.SLIME_BLOCK.defaultBlockState(), 3);
            level.setBlock(origin.above(), Blocks.CHEST.defaultBlockState(), 3);
            ((ChestBlockEntity)level.getBlockEntity(origin.above())).setItem(0, new ItemStack(Items.DIAMOND, 7));
            var result = assemble.invoke(null, level, origin, origin, true, true);
            if (result == null) throw new AssertionError("Assembly returned null at " + y);
            var ship = (SubLevel)result.getClass().getMethod("subLevel").invoke(result);
            var offset = (BlockPos)result.getClass().getMethod("offset").invoke(result);
            var local = origin.offset(offset);
            if (!level.getBlockState(origin).isAir() || !level.getBlockState(origin.above()).isAir())
                throw new AssertionError("Assembly left source blocks at " + y);
            var moved = (ChestBlockEntity)level.getBlockEntity(local.above());
            if (moved == null || !moved.getItem(0).is(Items.DIAMOND) || moved.getItem(0).getCount() != 7)
                throw new AssertionError("Assembly lost chest inventory at " + y);
            var flight = FlightMechanicsProbe.inspect(level, ship, local, y);
            double lift = flight.getAsJsonObject("moving").get("lift").getAsDouble();
            if (lift >= previousLift) throw new AssertionError("Lift did not decrease with altitude");
            previousLift = lift;
            disassemble.invoke(null, level, ship, local, origin, Rotation.NONE, false);
            var restored = (ChestBlockEntity)level.getBlockEntity(origin.above());
            if (!level.getBlockState(origin).is(Blocks.SLIME_BLOCK) || restored == null
                    || !restored.getItem(0).is(Items.DIAMOND) || restored.getItem(0).getCount() != 7)
                throw new AssertionError("Disassembly did not restore blocks/inventory at " + y);
            var row = new JsonObject(); row.addProperty("y", y);
            row.addProperty("assembled", true); row.addProperty("disassembled", true);
            row.addProperty("inventoryPreserved", true); rows.add(row);
            row.add("nativeFlightMethods", flight);
            level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
            System.out.println("MECHANICS_ASSEMBLY " + row);
        }
        return rows;
    }
}

package design.inherent.createskycompat.linear.client;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Authored animation, not a second physics simulation. Real mechanics have native fixtures. */
public final class LinearScenes {
    private LinearScenes() {}

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState();
    }
    private static CreateSceneBuilder start(SceneBuilder builder, SceneBuildingUtil util, String id, String title) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("linear_" + id, title);
        scene.configureBasePlate(0, 0, 7);
        scene.world().setBlock(new BlockPos(6, 5, 6), Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 6, 0, 6), Blocks.SMOOTH_STONE.defaultBlockState(), false);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        return scene;
    }
    private static void explain(CreateSceneBuilder scene, String text, Vec3 target) {
        scene.overlay().showText(80).attachKeyFrame().text(text).pointAt(target).placeNearTarget();
        scene.idle(90);
    }
    private static void rails(CreateSceneBuilder scene, SceneBuildingUtil util) {
        var rail = block("linearbearing:linear_casing").setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        scene.world().setBlocks(util.select().fromTo(3, 1, 1, 3, 1, 5), rail, false);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(15);
    }
    private static ElementLink<WorldSectionElement> carriage(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world().setBlock(new BlockPos(3, 2, 3), block("linearbearing:linear_moving")
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), false);
        scene.world().setBlocks(util.select().fromTo(3, 3, 3, 3, 4, 3), Blocks.OAK_PLANKS.defaultBlockState(), false);
        var moving = scene.world().showIndependentSection(util.select().fromTo(3, 2, 3, 3, 4, 3), Direction.DOWN);
        scene.world().moveSection(moving, new Vec3(0, -1, 0), 0);
        scene.idle(15);
        return moving;
    }

    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = start(builder, util, "assembly", "Assembling a Linear Bearing");
        rails(scene, util);
        var bearing = new BlockPos(3, 1, 3);
        scene.world().setBlock(bearing, block("linearbearing:linear_bearing")
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), false);
        explain(scene, "Linear Casings form a passive guide. Match the bearing and casing orientations.", bearing.getCenter());
        scene.world().setBlocks(util.select().fromTo(3, 2, 3, 3, 3, 3), Blocks.OAK_PLANKS.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(3, 2, 3, 3, 3, 3), Direction.DOWN);
        scene.overlay().showControls(new Vec3(3.5, 3, 3.5), Pointing.RIGHT, 40)
                .withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:super_glue"))));
        scene.effects().superGlue(new BlockPos(3, 2, 3), Direction.UP, true);
        explain(scene, "Glue the payload together above the bearing.", new Vec3(3.5, 3, 3.5));
        scene.overlay().showControls(bearing.getCenter(), Pointing.RIGHT, 40).rightClick();
        explain(scene, "Right-click the bearing with an empty hand to assemble the payload.", bearing.getCenter());
        scene.world().hideSection(util.select().fromTo(3, 2, 3, 3, 3, 3), Direction.UP);
        scene.idle(20);
        scene.world().setBlock(bearing, block("linearbearing:linear_casing")
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), false);
        carriage(scene, util);
        explain(scene, "The bearing becomes a casing. The assembled carriage can slide along matching rails.", bearing.getCenter());
        scene.markAsFinished();
    }

    public static void brake(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = start(builder, util, "brake", "Moving and Braking a Carriage");
        rails(scene, util);
        var moving = carriage(scene, util);
        scene.world().moveSection(moving, new Vec3(0, 0, -1), 50);
        explain(scene, "Gravity or an external force moves the carriage. The guide supplies no driving power.", new Vec3(3.5, 2, 2.5));
        scene.world().setBlock(new BlockPos(4, 3, 3), Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), false);
        scene.world().showSectionAndMerge(util.select().position(4, 3, 3), Direction.WEST, moving);
        scene.overlay().showControls(new Vec3(4.5, 2.5, 2.5), Pointing.LEFT, 40).rightClick();
        scene.world().toggleRedstonePower(util.select().position(4, 3, 3));
        explain(scene, "Redstone power on the moving carriage engages its brake at the current position.", new Vec3(3.5, 1.5, 2.5));
        scene.world().toggleRedstonePower(util.select().position(4, 3, 3));
        scene.world().moveSection(moving, new Vec3(0, 0, 2), 60);
        explain(scene, "Remove power to release the brake. Keep any powered drive separate from the brake circuit.", new Vec3(3.5, 2, 4.5));
        scene.markAsFinished();
    }

    public static void recovery(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = start(builder, util, "recovery", "Recovering a Carriage");
        rails(scene, util);
        var moving = carriage(scene, util);
        explain(scene, "Engage the brake and align the payload with the block grid before recovery.", new Vec3(3.5, 2, 3.5));
        var casing = new BlockPos(3, 1, 3);
        scene.overlay().showControls(casing.getCenter(), Pointing.RIGHT, 40).rightClick();
        explain(scene, "Right-click the casing with an empty hand to recover one aligned, braked carriage.", casing.getCenter());
        scene.world().hideIndependentSection(moving, Direction.UP);
        scene.idle(20);
        scene.world().setBlock(casing, block("linearbearing:linear_bearing")
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), false);
        scene.world().setBlocks(util.select().fromTo(3, 2, 3, 3, 3, 3), Blocks.OAK_PLANKS.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(3, 2, 3, 3, 3, 3), Direction.DOWN);
        explain(scene, "Recovery preserves the payload. Occupied spaces or a misaligned carriage prevent it.", casing.getCenter());
        explain(scene, "Recover the assembly before removing the rails you intend to reuse.", new Vec3(3.5, 1.5, 4.5));
        scene.markAsFinished();
    }

    public static void magnetic(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = start(builder, util, "magnetic", "Transmitting Rotation with Magnetic Ports");
        var a = new BlockPos(2, 1, 3); var b = new BlockPos(4, 1, 3);
        scene.world().setBlock(a, block("linearbearing:magnetic_port").setValue(BlockStateProperties.FACING, Direction.EAST), false);
        scene.world().setBlock(b, block("linearbearing:magnetic_port").setValue(BlockStateProperties.FACING, Direction.WEST), false);
        scene.world().setBlock(new BlockPos(1, 1, 3), block("create:shaft").setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(new BlockPos(5, 1, 3), block("create:shaft").setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        explain(scene, "Place two Magnetic Ports facing each other across a clear gap.", a.getCenter());
        scene.overlay().showOutline(PonderPalette.GREEN, "ports", util.select().fromTo(2, 1, 3, 4, 1, 3), 60);
        scene.world().modifyKineticSpeed(util.select().layer(1), speed -> 32f);
        explain(scene, "Supply rotation through a rear shaft. The facing port transfers it to the receiving network.", b.getCenter());
        explain(scene, "The source supplies the connected load. Overloading it stalls the network.", new Vec3(5.5, 1.5, 3.5));
        scene.world().modifyKineticSpeed(util.select().layer(1), speed -> 0f);
        explain(scene, "Magnetic Ports transfer rotation. They do not pull a carriage like Aeronautics' Redstone Magnets.", a.getCenter());
        scene.markAsFinished();
    }

    public static void spring(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = start(builder, util, "spring", "Linking Torsional Anchors");
        var a = new BlockPos(1, 1, 3); var b = new BlockPos(5, 1, 3);
        scene.world().setBlock(a, block("linearbearing:torsional_anchor").setValue(BlockStateProperties.FACING, Direction.EAST), false);
        scene.world().setBlock(b, block("linearbearing:torsional_anchor").setValue(BlockStateProperties.FACING, Direction.WEST), false);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        explain(scene, "Torsional Anchors transfer rotation through a spring connection.", a.getCenter());
        var spring = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("simulated:spring")));
        scene.overlay().showControls(a.getCenter(), Pointing.RIGHT, 40).rightClick().withItem(spring);
        scene.world().setBlock(a.east(), block("simulated:spring").setValue(BlockStateProperties.FACING, Direction.EAST), false);
        scene.world().setBlock(b.west(), block("simulated:spring").setValue(BlockStateProperties.FACING, Direction.WEST), false);
        scene.world().showSection(util.select().position(a.east()).add(util.select().position(b.west())), Direction.DOWN);
        explain(scene, "Place a Spring Head in front of each anchor, then connect the two heads.", b.getCenter());
        scene.world().modifyBlockEntity(a.east(), dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity.class,
                head -> head.setPartnerPos(b.west(), null));
        scene.world().modifyBlockEntity(b.west(), dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity.class,
                head -> head.setPartnerPos(a.east(), null));
        scene.overlay().showOutline(PonderPalette.GREEN, "spring", util.select().fromTo(2, 1, 3, 4, 1, 3), 70);
        scene.world().modifyKineticSpeed(util.select().layer(1), speed -> 32f);
        explain(scene, "Drive one anchor from a kinetic network. The linked anchor carries its rotation and load.", b.getCenter());
        explain(scene, "This is a rotational connection, not a motor that extends a linear carriage.", a.getCenter());
        scene.markAsFinished();
    }

    public static void gyro(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = start(builder, util, "gyro", "Reading Angular Velocity");
        var sensor = new BlockPos(3, 2, 3); var converter = new BlockPos(4, 2, 3);
        scene.world().setBlock(new BlockPos(3, 1, 3), Blocks.OAK_PLANKS.defaultBlockState(), false);
        scene.world().setBlock(sensor, block("linearbearing:gyroscopic_gearbox").setValue(BlockStateProperties.FACING, Direction.EAST), false);
        scene.world().setBlock(converter, block("linearbearing:redstone_converter").setValue(BlockStateProperties.FACING, Direction.EAST), false);
        var body = scene.world().showIndependentSection(util.select().fromTo(3, 1, 3, 4, 2, 3), Direction.DOWN);
        explain(scene, "Mount the Gyroscopic Gearbox on a simulated body to measure its angular velocity.", sensor.getCenter());
        scene.world().configureCenterOfRotation(body, new Vec3(3.5, 1.5, 3.5));
        scene.world().rotateSection(body, 45, 0, 0, 60);
        scene.world().modifyKineticSpeed(util.select().position(sensor), speed -> 32f);
        explain(scene, "Its shaft signal follows rotation about the selected axis. It has no stress capacity and supplies no driving power.", sensor.getCenter());
        explain(scene, "A Redstone Converter reads one signal level per 8 RPM, up to 15. Use the signal in a separate control circuit.", converter.getCenter());
        scene.world().modifyKineticSpeed(util.select().position(sensor), speed -> 0f);
        explain(scene, "When the body stops rotating, the sensor returns to zero. It does not hold an angle or stabilize the body.", sensor.getCenter());
        scene.markAsFinished();
    }
}

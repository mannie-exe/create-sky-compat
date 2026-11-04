package design.inherent.skyweavecompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import design.inherent.skyweavecompat.linear.BrakedCarriage;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Explicit disposable integrated-server scene; never included in the released artifact. */
final class ClientLinearProbe {
    private static final BlockPos BASE = new BlockPos(0, 100, 0);
    private boolean opened, placed;
    private int ticks;

    ClientLinearProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            event.getDispatcher().register(Commands.literal("compatprobe_linear_use").executes(context -> {
                var mc = Minecraft.getInstance();
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(BASE.getCenter(), Direction.UP, BASE, false));
                return 1;
            }));
            event.getDispatcher().register(Commands.literal("compatprobe_linear_brake").executes(context -> {
                var server = Minecraft.getInstance().getSingleplayerServer();
                server.execute(() -> {
                    var level = server.overworld();
                    for (var body : SubLevelContainer.getContainer(level).getAllSubLevels())
                        for (var actor : body.getPlot().getBlockEntityActors())
                            if (actor instanceof net.minecraft.world.level.block.entity.BlockEntity be
                                    && be instanceof BrakedCarriage)
                                level.setBlock(be.getBlockPos().above(), level.getBlockState(be.getBlockPos().above()).is(Blocks.REDSTONE_BLOCK)
                                        ? Blocks.STONE.defaultBlockState() : Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                });
                return 1;
            }));
        });
    }

    private void clientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (opened || mc.getOverlay() != null || !(mc.screen instanceof TitleScreen)) return;
        opened = true;
        String name = "linear-bearing-review";
        if (Files.isDirectory(FMLPaths.GAMEDIR.get().resolve("saves/" + name))) {
            placed = true;
            mc.createWorldOpenFlows().openWorld(name, () -> mc.setScreen(new TitleScreen()));
        } else mc.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings("Linear Bearing review", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .get(ResourceLocation.withDefaultNamespace("flat")).createWorldDimensions(), mc.screen);
    }

    private void serverTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        var level = server.overworld();
        if (!placed) {
            placed = true;
            for (var at : BlockPos.betweenClosed(BASE.offset(-5, -1, -9), BASE.offset(8, -1, 9)))
                level.setBlock(at, Blocks.STONE_BRICKS.defaultBlockState(), 3);
            var rails = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_casing")).defaultBlockState()
                    .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            for (int z = -6; z <= 6; z++) level.setBlock(BASE.offset(0, 0, z), rails, 3);
            var bearing = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_bearing")).defaultBlockState()
                    .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            level.setBlock(BASE, bearing, 3);
            level.setBlock(BASE.above(), Blocks.STONE.defaultBlockState(), 3);
            var player = server.getPlayerList().getPlayers().getFirst();
            player.teleportTo(2.5, 101, 3.5); player.setYRot(145); player.setXRot(35);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.displayClientMessage(Component.literal("Linear Bearing review"), true);
            level.setDayTime(6000);
        }
        if (++ticks % 20 != 0) return;
        var result = new JsonObject();
        result.addProperty("base", BuiltInRegistries.BLOCK.getKey(level.getBlockState(BASE).getBlock()).toString());
        result.addProperty("bodies", SubLevelContainer.getContainer(level).getAllSubLevels().size());
        for (var body : SubLevelContainer.getContainer(level).getAllSubLevels())
            for (var actor : body.getPlot().getBlockEntityActors())
                if (actor instanceof net.minecraft.world.level.block.entity.BlockEntity be && be instanceof BrakedCarriage carriage) {
                    result.addProperty("braked", carriage.skyweave$brake() != null);
                    Vec3 world = Sable.HELPER.projectOutOfSubLevel(level, be.getBlockPos().getCenter());
                    result.addProperty("carriage", world.toString());
                }
        try {
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("linear-client-state.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
    }
}

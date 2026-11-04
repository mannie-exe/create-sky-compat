package design.inherent.skyweavecompat.clientprobe;

import com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadBlockEntity;
import com.google.gson.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.commands.Commands;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.*;
import java.nio.file.*;
import java.util.List;

/** Explicit isolated world for visual/input checks, with real assembled endpoints held in view. */
final class ClientLinkageProbe {
    private boolean opened, placed;
    ClientLinkageProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            event.getDispatcher().register(Commands.literal("compatprobe_linkage_status").executes(context -> {
                var server=Minecraft.getInstance().getSingleplayerServer();
                server.execute(() -> {
                    var out=new JsonArray();
                    for(int z:new int[]{0,4}) {
                        var head=(HydraulicConnectionHeadBlockEntity)server.overworld().getBlockEntity(new BlockPos(0,100,z));
                        var row=new JsonObject();row.addProperty("z",z);row.addProperty("linked",head.hasLink());
                        out.add(row);
                    }
                    try {Files.writeString(FMLPaths.GAMEDIR.get().resolve("linkage-client-status.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));}
                    catch(Exception failure){throw new IllegalStateException(failure);}
                });return 1;
            }));
        });
    }
    private void clientTick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(opened || mc.getOverlay()!=null || !(mc.screen instanceof TitleScreen))return;
        opened=true;
        if(Files.isDirectory(FMLPaths.GAMEDIR.get().resolve("saves/transmission-linkage-review"))) {
            placed=true;
            mc.createWorldOpenFlows().openWorld("transmission-linkage-review",()->mc.setScreen(new TitleScreen()));
            return;
        }
        mc.createWorldOpenFlows().createFreshLevel("transmission-linkage-review",
                new LevelSettings("Transmission & Linkage review",GameType.CREATIVE,false,Difficulty.PEACEFUL,
                        true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .get(ResourceLocation.withDefaultNamespace("flat")).createWorldDimensions(),mc.screen);
    }
    private void serverTick(ServerTickEvent.Post event) {
        var server=event.getServer();
        if(placed || server.getPlayerCount()==0)return;
        placed=true;
        var level=server.overworld();
        for(var pos:BlockPos.betweenClosed(-3,99,-4,10,99,7))level.setBlock(pos,Blocks.STONE_BRICKS.defaultBlockState(),3);
        for(int z:new int[]{0,4}) {
            var at=new BlockPos(0,100,z);var moving=at.east(5);
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("aeronautics_utility_objects:hydraulic_connection_head"));
            level.setBlock(at,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST),3);
            level.setBlock(moving,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.WEST),3);
            var body=SubLevelAssemblyHelper.assembleBlocks(level,moving,List.of(moving),BoundingBox3i.from(List.of(moving)));
            var plot=body.getPlot().getCenterBlock();
            var head=(HydraulicConnectionHeadBlockEntity)level.getBlockEntity(at);
            var peer=(HydraulicConnectionHeadBlockEntity)level.getBlockEntity(plot);
            head.createMutualLink(peer,false,z==4);
            var world=Sable.HELPER.projectOutOfSubLevel(level,plot.getCenter());
            SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(body,null,
                    new FixedConstraintConfiguration(new Vector3d(plot.getX()+.5,plot.getY()+.5,plot.getZ()+.5),
                            new Vector3d(world.x,world.y,world.z),new Quaterniond(body.logicalPose().orientation()).conjugate()));
        }
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        var player=server.getPlayerList().getPlayers().getFirst();
        player.teleportTo(2.5,100,-2);player.setYRot(0);player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench"))));
    }
}

package design.inherent.createskycompat.clientprobe;

import com.google.gson.*;
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
import java.nio.file.*;
import java.util.List;

/** Explicit isolated world for visual/input checks, with real assembled endpoints held in view. */
final class ClientRedstoneProbe {
    private boolean opened, placed;
    ClientRedstoneProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
    }
    private void clientTick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(opened || mc.getOverlay()!=null || !(mc.screen instanceof TitleScreen))return;
        opened=true;
        if(Files.isDirectory(FMLPaths.GAMEDIR.get().resolve("saves/redstone-review"))) {
            placed=true;
            mc.createWorldOpenFlows().openWorld("redstone-review",()->mc.setScreen(new TitleScreen()));
            return;
        }
        mc.createWorldOpenFlows().createFreshLevel("redstone-review",
                new LevelSettings("Redstone review",GameType.CREATIVE,false,Difficulty.PEACEFUL,
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
        int x=0;
        for(String id : new String[]{"create:redstone_link", "create_redstone_additions:inverter", "create_redstone_additions:crossroad", "create_redstone_additions:conjunctor"}) {
            var at=new BlockPos(x,100,0);var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            var state=block.defaultBlockState();
            if(state.hasProperty(BlockStateProperties.FACING)) state=state.setValue(BlockStateProperties.FACING,Direction.UP);
            level.setBlock(at,state,3);x+=2;
        }
        var player=server.getPlayerList().getPlayers().getFirst();
        for(String id : new String[]{"minecraft:diamond", "minecraft:emerald", "minecraft:redstone", "create:redstone_link", "create:wrench", "create_redstone_additions:inverter"})
            player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),8));
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        player.teleportTo(2.5,100,-2);player.setYRot(0);player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench"))));
    }
}

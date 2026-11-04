package linearreview;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.*;
import com.simibubi.create.content.redstone.link.*;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import design.inherent.skyweavecompat.redstone.RedstoneEditEvent;

public final class RedstoneGuiProbe {
    public static void check(ServerLevel level,JsonObject report,boolean baseline) {
        var pos = new BlockPos(0,100,0);
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:redstone_link")).defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP);
        
        var player = FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"GuiFixture"));
        player.setPos(0.5,100,2.5); player.setGameMode(GameType.SURVIVAL);
        var menu = new RedstoneLinkMenu(1,player.getInventory(),pos); player.containerMenu=menu;
        IPayloadContext context = (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),new Class[]{IPayloadContext.class},(proxy,method,args)-> {
            if(method.getName().equals("player")) return player;
            if(method.getName().equals("enqueueWork")) { ((Runnable)args[0]).run(); return CompletableFuture.completedFuture(null); }
            throw new UnsupportedOperationException(method.toString());
        });
        var behaviour = BlockEntityBehaviour.get(level.getBlockEntity(pos),LinkBehaviour.TYPE);
        require(menu.stillValid(player),"fresh native menu invalid");
        RedstoneLinkFrequencyPayload.handleServer(new RedstoneLinkFrequencyPayload(pos,new ItemStack(Items.DIAMOND),0),context);
        require(com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper.getFrequencyItem(behaviour,0).is(Items.DIAMOND),"authorized frequency edit rejected");
        report.addProperty("authorizedFrequency",true);
        player.setGameMode(GameType.ADVENTURE);
        RedstoneLinkFrequencyPayload.handleServer(new RedstoneLinkFrequencyPayload(pos,new ItemStack(Items.EMERALD),0),context);
        require(com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper.getFrequencyItem(behaviour,0).is(baseline?Items.EMERALD:Items.DIAMOND),"adventure behavior differs from expected baseline");
        report.addProperty("adventureEditAccepted",baseline);
        if(baseline) return;
        player.setGameMode(GameType.SURVIVAL);
        var held = new ItemStack(Items.GOLD_INGOT,20); menu.setCarried(held);
        menu.clicked(0,0,ClickType.PICKUP,player);
        require(held.getCount()==20 && com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper.getFrequencyItem(behaviour,0).is(Items.GOLD_INGOT),"ghost slot changed inventory or failed edit");
        report.addProperty("ghostClickPreservesCount",true);
        java.util.function.Consumer<RedstoneEditEvent> deny = e->e.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(deny);
        RedstoneLinkFrequencyPayload.handleServer(new RedstoneLinkFrequencyPayload(pos,new ItemStack(Items.REDSTONE),0),context);
        require(com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper.getFrequencyItem(behaviour,0).is(Items.GOLD_INGOT),"protection veto ignored");
        NeoForge.EVENT_BUS.unregister(deny);
        report.addProperty("protectionVeto",true);
        player.setGameMode(GameType.SPECTATOR);
        RedstoneLinkFrequencyPayload.handleServer(new RedstoneLinkFrequencyPayload(pos,new ItemStack(Items.REDSTONE),0),context);
        require(com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper.getFrequencyItem(behaviour,0).is(Items.GOLD_INGOT),"spectator mutated link");
        player.setGameMode(GameType.SURVIVAL);
        for(int row=0;row<4;row++) {
            PresetSlotUpdatePayload.handleServer(new PresetSlotUpdatePayload(row,0,new ItemStack(Items.COAL)),context);
            require(com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData.get(player).getStack(row,0).is(Items.COAL),"valid preset row denied");
        }
        RedstoneLinkFrequencyPayload.handleServer(new RedstoneLinkFrequencyPayload(pos,new ItemStack(Items.REDSTONE),5),context);
        require(com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper.getFrequencyItem(behaviour,1).isEmpty(),"invalid frequency slot accepted");
        report.addProperty("spectatorAndInvalidSlotsDeniedFourPresetsAllowed",true);
        player.setPos(100,100,100);
        require(!menu.stillValid(player),"distant menu valid");
        menu.clicked(0,0,ClickType.PICKUP,player);
        require(com.ggrgg.createredstonelinkgui.common.menu.FrequencyHelper.getFrequencyItem(behaviour,0).is(Items.GOLD_INGOT),"distant ghost click changed link");
        report.addProperty("distantMenuAndNativeClickDenied",true);
        player.setPos(0.5,100,2.5);
        
        // Moving intentionally starts after the client closes its menu.
        player.containerMenu=player.inventoryMenu;
        var target=pos.east(3); level.setBlock(target.below(),Blocks.STONE.defaultBlockState(),3);
        var moved=new RedstoneLinkMovePayload(pos,target,Direction.UP);
        player.setGameMode(GameType.ADVENTURE);
        RedstoneLinkMovePayload.handleServer(moved,context);
        require(!level.getBlockState(pos).isAir() && level.getBlockState(target).isAir(),"adventure relocation applied");
        player.setGameMode(GameType.SURVIVAL);
        NeoForge.EVENT_BUS.addListener(deny);
        RedstoneLinkMovePayload.handleServer(moved,context);
        require(!level.getBlockState(pos).isAir() && level.getBlockState(target).isAir(),"protected relocation applied");
        NeoForge.EVENT_BUS.unregister(deny);
        RedstoneLinkMovePayload.handleServer(moved,context);
        require(level.getBlockState(pos).isAir() && level.getBlockState(target).is(state.getBlock()),"ordinary menu-closed relocation failed");
        RedstoneLinkMovePayload.handleServer(moved,context);
        require(level.getBlockState(pos).isAir() && level.getBlockState(target).is(state.getBlock()),"repeat relocation duplicated block");
        report.addProperty("relocationAllowsPlainLinkOnceWithNoMenu",true);
        report.addProperty("relocationRejectsAdventureAndProtectionVeto",true);
        level.setBlock(pos,state,3);
        require(!menu.stillValid(player),"replaced block kept old menu valid");
        report.addProperty("replacedIdentityDenied",true);
    }
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
}

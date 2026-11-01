package linearreview;

import com.enxv.aerouniversaljoint.content.*;
import com.enxv.aerouniversaljoint.network.*;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import design.inherent.createskycompat.linkage.LinkPermissions;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Bounded native packet/permission evidence, not a substitute for interactive flight checks. */
@Mod("linear_review_probe")
public final class LinkageProbe {
    private final JsonObject report = new JsonObject();
    public LinkageProbe() { NeoForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        try { check(event.getServer().overworld());
            Files.writeString(Path.of(System.getProperty("linear.review.output")), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Throwable failure) {
            failure.printStackTrace();
            try { Files.writeString(Path.of(System.getProperty("linear.review.output") + ".failure"), failure.toString()); }
            catch (Exception ignored) {}
        }
        event.getServer().halt(false);
    }

    private void check(ServerLevel level) throws Exception {
        boolean baseline = Boolean.getBoolean("linkage.review.baseline");
        BlockPos fixed = new BlockPos(100,150,0), moving = fixed.east(5);
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("aeronautics_utility_objects:hydraulic_connection_head"));
        level.setBlock(fixed, block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST), 3);
        level.setBlock(moving, block.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.WEST), 3);
        var positions = List.of(moving);
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, moving, positions, BoundingBox3i.from(positions));
        var head = (HydraulicConnectionHeadBlockEntity)level.getBlockEntity(fixed);
        var peer = (HydraulicConnectionHeadBlockEntity)level.getBlockEntity(ship.getPlot().getCenterBlock());
        require(head != null && peer != null, "missing actual head block entities");
        var link = head.createMutualLink(peer);
        require(head.hasLink() && peer.hasLink(), "native link creation failed: " + link);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("7132f4c7-40b9-437c-92c8-c1d572f14aff"), "LinkageFixture"));
        player.setGameMode(GameType.SURVIVAL);
        // Eye at the rod's height, looking along +Z at its midpoint.
        player.setPos(103,150.5-player.getEyeHeight(),-2);
        player.setYRot(0); player.setXRot(0);
        require(HydraulicRodInteraction.isTarget(player, head) || HydraulicRodInteraction.isTarget(player, peer), "fixture does not hit actual rod");
        var selected = HydraulicRodInteraction.isTarget(player, head) ? head : peer;
        IPayloadContext context = (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class[]{IPayloadContext.class}, (proxy, method, args) -> {
            if (method.getName().equals("player")) return player;
            if (method.getName().equals("enqueueWork")) { ((Runnable)args[0]).run(); return CompletableFuture.completedFuture(null); }
            throw new UnsupportedOperationException(method.toString());
        });
        var packet = new BreakHydraulicRodPayload(selected.getBlockPos(), selected.getContainingSubLevelId());
        BreakHydraulicRodPayload.handleServer(packet, context);
        report.addProperty("bareAttackDetached", !head.hasLink());
        require(baseline == !head.hasLink(), "bare removal behavior does not match expected baseline/fix");
        if (baseline) return;

        player.setShiftKeyDown(true); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench"))));
        player.setGameMode(GameType.ADVENTURE);
        BreakHydraulicRodPayload.handleServer(packet, context);
        require(head.hasLink(), "adventure player detached link"); report.addProperty("adventureDenied",true);
        player.setGameMode(GameType.SPECTATOR);
        BreakHydraulicRodPayload.handleServer(packet, context);
        require(head.hasLink(), "spectator detached link"); report.addProperty("spectatorDenied",true);
        player.setGameMode(GameType.SURVIVAL);
        var denied = peer.getBlockPos();
        java.util.function.Consumer<BlockEvent.BreakEvent> denyPeer = e -> {if (e.getPos().equals(denied)) e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(denyPeer);
        BreakHydraulicRodPayload.handleServer(packet, context);
        require(head.hasLink() && peer.hasLink(), "protected opposite endpoint detached"); report.addProperty("peerProtectionDenied",true);
        NeoForge.EVENT_BUS.unregister(denyPeer);

        player.containerMenu = new HydraulicConnectionHeadMenu(7, player.getInventory(), head, true);
        int before = head.getExpectedLengthTenths();
        var settings = new SetHydraulicSettingsPayload(fixed, 2, false, before+10, 1, 10, 150);
        player.setGameMode(GameType.ADVENTURE);
        SetHydraulicSettingsPayload.handleServer(settings,context);
        require(head.getExpectedLengthTenths()==before,"adventure settings packet mutated link"); report.addProperty("settingsAdventureDenied",true);
        player.setGameMode(GameType.SURVIVAL);
        SetHydraulicSettingsPayload.handleServer(settings,context);
        require(head.getExpectedLengthTenths()==before+10,"authorized settings not applied"); report.addProperty("settingsAllowed",true);
        player.setPos(1000,150,1000);
        SetHydraulicSettingsPayload.handleServer(new SetHydraulicSettingsPayload(fixed,2,false,before+20,1,10,150),context);
        require(head.getExpectedLengthTenths()==before+10,"stale distant menu mutated link");report.addProperty("staleMenuDenied",true);
        player.setPos(103,150.5-player.getEyeHeight(),-2);
        BreakHydraulicRodPayload.handleServer(packet,context);
        require(!head.hasLink() && !peer.hasLink(),"authorized sneak wrench failed");report.addProperty("sneakWrenchDetachedBoth",true);
        BreakHydraulicRodPayload.handleServer(packet,context);
        require(!head.hasLink() && !peer.hasLink(),"duplicate removal restored link");report.addProperty("repeatRemovalNoop",true);
        player.setShiftKeyDown(false);
        var rod = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("aeronautics_utility_objects:hydraulic_rod")),2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rod);
        PendingHydraulicSelections.write(player,new JointBindingData.Selection(level.dimension().location(),peer.getBlockPos(),peer.getContainingSubLevelId()));
        java.util.function.Consumer<design.inherent.createskycompat.linkage.LinkageEditEvent> denyEdit = e -> {if(e.endpoint()==peer) e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(denyEdit);
        head.handleHeldHydraulicRodItem(player,net.minecraft.world.InteractionHand.MAIN_HAND);
        require(!head.hasLink() && !peer.hasLink() && rod.getCount()==2,"protected selection linked or consumed rod");
        report.addProperty("protectedBindingDenied",true);
        NeoForge.EVENT_BUS.unregister(denyEdit);
        head.handleHeldHydraulicRodItem(player,net.minecraft.world.InteractionHand.MAIN_HAND);
        require(head.hasLink() && peer.hasLink() && rod.getCount()==1,"authorized binding failed");
        report.addProperty("authorizedBindingConsumesOne",true);
    }
    private static void require(boolean value, String message) {if (!value) throw new AssertionError(message);}
}

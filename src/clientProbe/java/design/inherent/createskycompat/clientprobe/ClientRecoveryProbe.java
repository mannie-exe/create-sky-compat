package design.inherent.createskycompat.clientprobe;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import design.inherent.createskycompat.recovery.ShipGraves;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.nio.file.Files;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Real-player support, native death/respawn, ray targeting and equipped inventory recovery. */
final class ClientRecoveryProbe {
    private boolean opened, respawnRequested, clicked, captured;
    private int age, clientAge, expectedXp;
    private volatile int stage;
    private volatile boolean ready;
    private volatile BlockPos physical;
    private ServerSubLevel ship;
    private GraveComponent grave;
    private final JsonObject report = new JsonObject();

    ClientRecoveryProbe() {
        NeoForge.EVENT_BUS.addListener(this::client);
        NeoForge.EVENT_BUS.addListener(this::server);
    }
    private void client(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if (!opened && mc.getOverlay()==null && mc.screen instanceof TitleScreen) {
            opened=true;
            mc.createWorldOpenFlows().createFreshLevel("recovery-review",
                    new LevelSettings("Recovery review",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .get(ResourceLocation.withDefaultNamespace("flat")).createWorldDimensions(),mc.screen);
        }
        if (mc.player==null || mc.level==null) return;
        ready = mc.getOverlay()==null && mc.screen==null;
        if (mc.screen instanceof DeathScreen && !respawnRequested) { respawnRequested=true; mc.player.respawn(); }
        if (stage==3 && clientAge==0) {
            var view=new JsonObject();
            view.addProperty("eye",mc.player.getEyePosition().toString());
            view.addProperty("yaw",mc.player.getYRot());view.addProperty("pitch",mc.player.getXRot());
            view.addProperty("targetPlot",String.valueOf(physical));view.addProperty("hit",String.valueOf(mc.hitResult));
            if (mc.hitResult instanceof BlockHitResult hit) view.addProperty("hitBlock",hit.getBlockPos().toString());
            view.addProperty("screen",String.valueOf(mc.screen));
            try { Files.writeString(FMLPaths.GAMEDIR.get().resolve("recovery-view.json"),view.toString()); }
            catch(java.io.IOException failure) { throw new IllegalStateException(failure); }
            Screenshot.grab(mc.gameDirectory,"recovery-position.png",mc.getMainRenderTarget(),ignored -> {});
        }
        if (stage==3 && ++clientAge>40 && !clicked && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(physical)) {
            if (!captured) { captured=true; Screenshot.grab(mc.gameDirectory,"recovery-grave.png",mc.getMainRenderTarget(),ignored -> {}); }
            clicked=true;
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit);
        }
        if (stage==4) mc.stop();
    }
    private void server(ServerTickEvent.Post event) {
        var server=event.getServer();
        if (stage==4 || server.getPlayerCount()==0) return;
        try {
            age++;
            var level=server.overworld();
            var player=server.getPlayerList().getPlayers().getFirst();
            if (stage==0 && ready && age>40) {
                level.setChunkForced(7,0,true);
                var blocks=new ArrayList<BlockPos>();
                for (int x=116;x<=124;x++) for (int z=-4;z<=4;z++) {
                    var pos=new BlockPos(x,120,z);blocks.add(pos);level.setBlockAndUpdate(pos,Blocks.OAK_PLANKS.defaultBlockState());
                }
                ship=SubLevelAssemblyHelper.assembleBlocks(level,new BlockPos(120,120,0),blocks,BoundingBox3i.from(blocks));
                var center=ship.getPlot().getCenterBlock();var world=ship.logicalPose().transformPosition(center.getCenter());
                SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(ship,null,
                        new FixedConstraintConfiguration(new Vector3d(center.getX()+.5,center.getY()+.5,center.getZ()+.5),
                                new Vector3d(world.x,world.y,world.z),new Quaterniond(ship.logicalPose().orientation()).conjugate()));
                var feet=ship.logicalPose().transformPosition(Vec3.atBottomCenterOf(center.above())).add(0,.5,0);
                player.teleportTo(level,feet.x,feet.y,feet.z,0,0);
                stage=1;
            }
            if (stage==1 && age>100) {
                require(player.onGround() && Sable.HELPER.getTrackingSubLevel(player)==ship,"real player not supported by ship");
                require(player.getPersistentData().contains("create_sky_compat:deck_support"),"natural support not recorded");
                report.addProperty("naturalCollisionSupport",true);
                player.getInventory().add(new ItemStack(Items.DIAMOND,7));
                player.giveExperiencePoints(280);
                expectedXp=player.getExperienceReward(level,null);
                Class.forName("fixture.BackpackRecovery").getMethod("equip",net.minecraft.server.level.ServerPlayer.class).invoke(null,player);
                player.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);
                grave=DeathInfoManager.INSTANCE.getBackupData(new ResolvableProfile(player.getGameProfile())).getLast();
                require(ShipGraves.anchor(grave)!=null,"native death not adapted");
                require(grave.getExpComponent().getStoredXp()==expectedXp,"stored XP differs from vanilla death drop");
                report.addProperty("storedVanillaXp",expectedXp);
                report.addProperty("nativeDeath",true);
                stage=2;
            }
            if (stage==2 && player.isAlive() && age>120) {
                physical=grave.getPos();
                require(level.getBlockEntity(physical) instanceof GraveBlockEntity,"native plot grave missing");
                require(level.getFluidState(physical).isEmpty(),"air placement introduced a fluid");
                var point=ShipGraves.worldPosition(grave);
                player.teleportTo(level,point.getX()+.5,point.getY(),point.getZ()+3.5,180,20.5f);
                stage=3;
            }
            if (stage==3 && grave.getStatus()==GraveStatus.CLAIMED) {
                require(clicked,"grave claimed without native client interaction");
                require(player.getInventory().countItem(Items.DIAMOND)==7,"death event changed item custody");
                require(player.totalExperience==expectedXp,"claim changed XP policy");
                Class.forName("fixture.BackpackRecovery").getMethod("restored",net.minecraft.server.level.ServerPlayer.class).invoke(null,player);
                report.addProperty("nativeRayAndClaim",true);
                report.addProperty("backpackNestedContentsAndEquipment",true);
                finish(server);
            }
            require(age<500,"real-player fixture timed out at stage "+stage);
        } catch(Throwable failure) {
            report.addProperty("failure",failure.toString());
            finish(server);
        }
    }
    private void finish(net.minecraft.server.MinecraftServer server) {
        try { Files.writeString(FMLPaths.GAMEDIR.get().resolve("recovery-review.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
        catch(java.io.IOException failure) { throw new IllegalStateException(failure); }
        stage=4;
        server.halt(false);
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}

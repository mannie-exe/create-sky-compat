package fixture;

import com.b1n_ry.yigd.DeathHandler;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import design.inherent.createskycompat.recovery.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.*;
import java.nio.file.*;
import java.util.*;
import org.joml.*;

/** Finite native ownership/placement fixture. It does not establish real-player collision support. */
@Mod("linear_review_probe")
public final class RecoveryProbe {
    private int tick;
    private boolean started;
    private ServerPlayer actor;
    private ServerSubLevel ship;
    private GraveComponent grave;
    private ServerSubLevel trialShip;
    private GraveComponent trialGrave;
    private PhysicsConstraintHandle trialPin;
    private boolean protect;
    private final JsonObject out = new JsonObject();
    private final GameProfile profile = new GameProfile(UUID.fromString("11b1cd84-7603-48a6-90af-a62f8841d145"), "RecoveryFixture");

    public RecoveryProbe() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> started = true);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((RecoveryPlacementEvent event) -> { if (protect) event.setCanceled(true); });
    }
    private void tick(ServerTickEvent.Post event) {
        if (!started) return;
        try {
            tick++;
            var level = event.getServer().overworld();
            boolean restore = Boolean.getBoolean("linear.review.restore");
            if (tick == 1) {
                level.setChunkForced(6, 0, true);
                actor = FakePlayerFactory.get(level, profile);
                actor.setGameMode(GameType.SURVIVAL);
                if (restore) {
                    var previous = JsonParser.parseString(Files.readString(Path.of(System.getProperty("linear.review.previous")))).getAsJsonObject();
                    BlockPos pos = BlockPos.of(previous.get("physical").getAsLong());
                    level.getChunkAt(pos);
                    grave = DeathInfoManager.INSTANCE.getGrave(UUID.fromString(previous.get("graveId").getAsString())).orElseThrow();
                    require(ShipGraves.anchor(grave) != null, "anchor lost on reopen");
                } else {
                    var positions = new ArrayList<BlockPos>();
                    for (int x = 98; x <= 102; x++) for (int z = -2; z <= 2; z++) {
                        var pos = new BlockPos(x, 100, z); positions.add(pos);
                        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                    }
                    ship = SubLevelAssemblyHelper.assembleBlocks(level, new BlockPos(100,100,0), positions, BoundingBox3i.from(positions));
                    var center = ship.getPlot().getCenterBlock();
                    var world = ship.logicalPose().transformPosition(center.getCenter());
                    SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(ship, null,
                            new FixedConstraintConfiguration(new Vector3d(center.getX()+.5,center.getY()+.5,center.getZ()+.5),
                                    new Vector3d(world.x,world.y,world.z),new Quaterniond(ship.logicalPose().orientation()).conjugate()));
                    var feet = ship.logicalPose().transformPosition(Vec3.atBottomCenterOf(center.above()));
                    actor.setPos(feet);
                    ((EntityMovementExtension)actor).sable$setTrackingSubLevel(ship);
                    actor.setOnGround(true);
                    NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(actor));
                    require(actor.getPersistentData().contains("create_sky_compat:deck_support"), "support not recorded: grounded="+actor.onGround()
                            +" tracking="+dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(actor)
                            +" feet="+feet+" inverse="+ship.logicalPose().transformPositionInverse(actor.position())
                            +" center="+center+" state="+level.getBlockState(center)
                            +" holder="+ship.getPlot().getChunkHolder(new net.minecraft.world.level.ChunkPos(center)));
                    actor.getInventory().add(new ItemStack(Items.DIAMOND, 7));
                    backpack("equip");
                    actor.setOnGround(false);
                    var death = new Vec3(feet.x, level.getMinBuildHeight()-80, feet.z);
                    actor.setPos(death);
                    var handler = new DeathHandler(actor, level, death, level.damageSources().fellOutOfWorld());
                    handler.finalizeDeath();
                    backpack("cleared");
                    grave = DeathInfoManager.INSTANCE.getBackupData(new ResolvableProfile(profile)).getLast();
                    require(ShipGraves.anchor(grave) != null, "death not adapted");
                }
            }
            if (tick == 10) {
                require(level.getBlockEntity(grave.getPos()) instanceof GraveBlockEntity, "grave not physically placed");
                require(level.getFluidState(grave.getPos()).isEmpty(), "air placement introduced a fluid");
                require(ShipGraves.currentPhysicalGrave(grave, level, grave.getPos()), "grave identity does not match its plot");
                var physical = grave.getPos();
                var projected = ShipGraves.worldPosition(grave);
                require(!physical.equals(projected), "display position exposed raw plot coordinates");
                out.addProperty("physical", physical.asLong());
                out.addProperty("graveId", grave.getGraveId().toString());
                out.addProperty("physicalPlotVerified", true);
                out.addProperty("displayProjected", true);
                out.addProperty("restored", restore);
                if (restore) {
                    actor.setPos(projected.getCenter());
                    var other = FakePlayerFactory.getMinecraft(level);
                    other.setPos(projected.getCenter());
                    require(grave.claim(other,level,Blocks.AIR.defaultBlockState(),physical,ItemStack.EMPTY)==InteractionResult.FAIL,
                            "non-owner claim accepted");
                    out.addProperty("nonOwnerClaimRefused",true);
                    var be = (GraveBlockEntity)level.getBlockEntity(physical);
                    var first = grave.claim(actor, level, be.getPreviousState(), physical, ItemStack.EMPTY);
                    require(first != InteractionResult.FAIL, "owner claim failed");
                    require(actor.getInventory().countItem(Items.DIAMOND) == 7, "inventory not restored once");
                    require(grave.claim(actor, level, Blocks.AIR.defaultBlockState(), physical, ItemStack.EMPTY) == InteractionResult.FAIL,
                            "repeat claim accepted");
                    require(actor.getInventory().countItem(Items.DIAMOND) == 7 && grave.getStatus() == GraveStatus.CLAIMED, "repeat claim duplicated inventory");
                    backpack("restored");
                    if (net.neoforged.fml.ModList.get().isLoaded("travelersbackpack")) out.addProperty("backpackAccessoryAndNestedContents", true);
                    out.addProperty("singleClaimVerified", true);
                }
                if (restore) finish(event);
            }
            if (!restore) runLossChecks(event, level);
        } catch (Throwable failure) {
            failure.printStackTrace();
            try { Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"), failure.toString()); } catch(Exception ignored) { }
            event.getServer().halt(false);
            started = false;
        }
    }
    private void backpack(String operation) throws Exception {
        if (net.neoforged.fml.ModList.get().isLoaded("travelersbackpack"))
            Class.forName("fixture.BackpackRecovery").getMethod(operation, ServerPlayer.class).invoke(null, actor);
    }
    private void runLossChecks(ServerTickEvent.Post event, ServerLevel level) throws Exception {
        if (tick == 20 || tick == 60 || tick == 100 || tick == 140 || tick == 180) {
            int x = 140 + tick * 2;
            level.setChunkForced(x >> 4, 0, true);
            var positions = new ArrayList<BlockPos>();
            for (int dx = -2; dx <= 2; dx++) for (int z = -2; z <= 2; z++) {
                var pos = new BlockPos(x+dx, 110, z); positions.add(pos);
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            trialShip = SubLevelAssemblyHelper.assembleBlocks(level, new BlockPos(x,110,0), positions, BoundingBox3i.from(positions));
            pin(level);
            actor.setPos(trialShip.logicalPose().transformPosition(Vec3.atBottomCenterOf(trialShip.getPlot().getCenterBlock().above())));
            ((EntityMovementExtension)actor).sable$setTrackingSubLevel(trialShip);
            actor.setOnGround(true);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(actor));
            require(actor.getPersistentData().contains("create_sky_compat:deck_support"), "trial support missing");
            if (tick == 20) SubLevelContainer.getContainer(level).removeSubLevel(trialShip, SubLevelRemovalReason.REMOVED);
            protect = tick == 100;
            actor.getInventory().clearContent();
            actor.getInventory().add(new ItemStack(Items.GOLD_INGOT, 3));
            actor.setOnGround(tick == 180);
            var death = tick == 180 ? actor.position() : new Vec3(x+.5, level.getMinBuildHeight()-80, .5);
            actor.setPos(death);
            new DeathHandler(actor, level, death, tick == 180 ? level.damageSources().generic() : level.damageSources().fellOutOfWorld()).finalizeDeath();
            trialGrave = DeathInfoManager.INSTANCE.getBackupData(new ResolvableProfile(profile)).getLast();
        }
        if (tick == 30) {
            require(ShipGraves.anchor(trialGrave).state().equals("world"), "removed construct did not use world fallback");
            var at = trialGrave.getPos();
            require(level.getBlockEntity(at) instanceof GraveBlockEntity && level.getBlockState(at.below()).is(Blocks.COBBLESTONE), "fallback is not safely supported");
            actor.setPos(at.getCenter());
            require(trialGrave.claim(actor, level, Blocks.AIR.defaultBlockState(), at, ItemStack.EMPTY) == InteractionResult.SUCCESS,
                    "fallback claim failed");
            require(actor.getInventory().countItem(Items.GOLD_INGOT) == 3, "fallback lost inventory");
            out.addProperty("constructLostBeforeDeath", true);
        }
        if (tick == 70) {
            require(ShipGraves.anchor(trialGrave).state().equals("ship"), "second plot grave missing");
            trialPin.remove();
            RigidBodyHandle.of(trialShip).teleport(new Vector3d(trialShip.logicalPose().position()).add(15,8,12), new Quaterniond().rotationY(java.lang.Math.PI/2));
        }
        if (tick == 72) {
            pin(level);
            var projected = ShipGraves.worldPosition(trialGrave);
            require(projected.getY() >= 118, "grave display did not follow transformed ship");
            com.b1n_ry.yigd.util.GraveCompassHelper.giveCompass(actor, trialGrave.getGraveId(), trialGrave.getPos(), level.dimension());
            var compass = actor.getInventory().items.stream().filter(stack -> stack.is(Items.COMPASS)).findFirst().orElseThrow();
            require(compass.get(com.b1n_ry.yigd.Yigd.GRAVE_LOCATION).pos().equals(projected), "compass exposed plot address");
            require(trialGrave.claim(actor, level, Blocks.AIR.defaultBlockState(), trialGrave.getPos(), ItemStack.EMPTY) == InteractionResult.FAIL,
                    "remote claim succeeded");
            actor.setPos(projected.getCenter());
            require(trialGrave.claim(actor, level, Blocks.AIR.defaultBlockState(), trialGrave.getPos(), ItemStack.EMPTY) == InteractionResult.SUCCESS,
                    "transformed grave could not be claimed");
            out.addProperty("rotatedTranslatedClaim", true);
            out.addProperty("compassProjected", true);
            out.addProperty("remoteClaimRefused", true);
        }
        if (tick == 110) {
            require(ShipGraves.anchor(trialGrave).state().equals("pending"), "protected placement was bypassed");
            require(!(level.getBlockEntity(trialGrave.getPos()) instanceof GraveBlockEntity), "protected grave placed");
            require(trialGrave.getInventoryComponent().graveSize() > 0, "blocked inventory was dropped");
            protect = false;
            out.addProperty("protectedPlacementRetainsCustody", true);
            // FakePlayer is deliberately not registered as an online player. Exercise the same deferred retry with it.
            retry();
            require(ShipGraves.anchor(trialGrave).state().equals("ship"), "unblocked deferred placement failed");
            SubLevelContainer.getContainer(level).removeSubLevel(trialShip, SubLevelRemovalReason.UNLOADED);
        }
        if (tick == 120) {
            require(ShipGraves.anchor(trialGrave).state().equals("unloaded"), "unload conflated with destruction");
            require(trialGrave.getInventoryComponent().graveSize() > 0, "unloaded recovery lost contents");
            out.addProperty("unloadRetainsSingleRecord", true);
        }
        if (tick == 150) {
            require(ShipGraves.anchor(trialGrave).state().equals("ship"), "placed grave missing before removal");
            SubLevelContainer.getContainer(level).removeSubLevel(trialShip, SubLevelRemovalReason.REMOVED);
            level.removeBlock(trialGrave.getPos(),false);
        }
        if (tick == 160) {
            BlockPos old = trialGrave.getPos();
            require(ShipGraves.anchor(trialGrave).state().equals("pending"), "removed grave was not retained pending");
            retry();
            require(ShipGraves.anchor(trialGrave).state().equals("world"), "removed grave not relocated safely");
            actor.setPos(trialGrave.getPos().getCenter());
            require(trialGrave.claim(actor,level,Blocks.AIR.defaultBlockState(),old,ItemStack.EMPTY)==InteractionResult.FAIL,"stale plot claim accepted");
            require(trialGrave.claim(actor,level,Blocks.AIR.defaultBlockState(),trialGrave.getPos(),ItemStack.EMPTY)==InteractionResult.SUCCESS,"relocated claim failed");
            require(actor.getInventory().countItem(Items.GOLD_INGOT)==3,"relocation changed inventory");
            out.addProperty("placedConstructRemoval",true);
            out.addProperty("stalePhysicalClaimRefused",true);
        }
        if (tick == 190) {
            require(ShipGraves.anchor(trialGrave).state().equals("ship"),"ordinary deck death was not adapted");
            out.addProperty("ordinaryDeckDeath",true);
            if (net.neoforged.fml.ModList.get().isLoaded("simulated")) {
                var helper=Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
                helper.getMethod("disassembleSubLevel",net.minecraft.world.level.Level.class,
                        dev.ryanhcode.sable.sublevel.SubLevel.class,BlockPos.class,BlockPos.class,
                        net.minecraft.world.level.block.Rotation.class,boolean.class)
                        .invoke(null,level,trialShip,trialShip.getPlot().getCenterBlock(),new BlockPos(500,110,0),
                                net.minecraft.world.level.block.Rotation.NONE,true);
            }
        }
        if (tick == 200 && net.neoforged.fml.ModList.get().isLoaded("simulated")) {
            require(ShipGraves.anchor(trialGrave).state().equals("world"),"native disassembly lost world association");
            require(level.getBlockEntity(trialGrave.getPos()) instanceof GraveBlockEntity,"disassembled physical grave missing");
            actor.setPos(trialGrave.getPos().getCenter());
            require(trialGrave.claim(actor,level,Blocks.AIR.defaultBlockState(),trialGrave.getPos(),ItemStack.EMPTY)==InteractionResult.SUCCESS,"disassembled claim failed");
            require(actor.getInventory().countItem(Items.GOLD_INGOT)==3,"disassembly changed inventory");
            out.addProperty("nativeDisassemblySingleClaim",true);
        }
        if (tick == 210) {
            level.setChunkForced(600 >> 4,0,true);
            level.setBlockAndUpdate(new BlockPos(600,100,0),Blocks.STONE.defaultBlockState());
            actor.setPos(600.5,101,.5);
            ((EntityMovementExtension)actor).sable$setTrackingSubLevel(null);
            actor.setOnGround(true);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(actor));
            actor.getInventory().clearContent();actor.getInventory().add(new ItemStack(Items.IRON_INGOT,6));
            new DeathHandler(actor,level,actor.position(),level.damageSources().generic()).finalizeDeath();
            trialGrave=DeathInfoManager.INSTANCE.getBackupData(new ResolvableProfile(profile)).getLast();
            require(ShipGraves.anchor(trialGrave)==null,"ordinary world death was intercepted");
        }
        if (tick == 220) {
            require(level.getBlockEntity(trialGrave.getPos()) instanceof GraveBlockEntity,"ordinary native grave missing");
            require(trialGrave.claim(actor,level,Blocks.AIR.defaultBlockState(),trialGrave.getPos(),ItemStack.EMPTY)==InteractionResult.SUCCESS,"ordinary native claim failed");
            require(actor.getInventory().countItem(Items.IRON_INGOT)==6,"ordinary death changed item custody");
            out.addProperty("ordinaryWorldDeathUnadapted",true);
            finish(event);
        }
    }
    private void retry() throws Exception {
        var retry = ShipGraves.class.getDeclaredMethod("place", GraveComponent.class, ServerPlayer.class, com.b1n_ry.yigd.components.RespawnComponent.class);
        retry.setAccessible(true); retry.invoke(null, trialGrave, actor, null);
    }
    private void pin(ServerLevel level) {
        var center = trialShip.getPlot().getCenterBlock();
        var world = trialShip.logicalPose().transformPosition(center.getCenter());
        trialPin = SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(trialShip, null,
                new FixedConstraintConfiguration(new Vector3d(center.getX()+.5,center.getY()+.5,center.getZ()+.5),
                        new Vector3d(world.x,world.y,world.z),new Quaterniond(trialShip.logicalPose().orientation()).conjugate()));
    }
    private void finish(ServerTickEvent.Post event) throws Exception {
        Files.writeString(Path.of(System.getProperty("linear.review.output")), new GsonBuilder().setPrettyPrinting().create().toJson(out));
        event.getServer().halt(false);
        started = false;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

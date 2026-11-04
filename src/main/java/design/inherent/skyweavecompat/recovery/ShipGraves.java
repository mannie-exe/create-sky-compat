package design.inherent.skyweavecompat.recovery;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.ExtraFeaturesConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.events.YigdEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-thread placement metadata and bounded recovery of existing YiGD records. */
public final class ShipGraves {
    private static final String SUPPORT = "skyweave_compat:deck_support";
    private static final Map<UUID, GraveComponent> TRACKED = new LinkedHashMap<>();
    private static final Set<UUID> PENDING_NOTICES = new HashSet<>();
    private static final Set<ServerLevel> OBSERVED = Collections.newSetFromMap(new WeakHashMap<>());

    private ShipGraves() { }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ShipGraves::sampleSupport);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ShipGraves::death);
        NeoForge.EVENT_BUS.addListener(ShipGraves::tick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerStartedEvent event) -> {
            TRACKED.clear();
            PENDING_NOTICES.clear();
            DeathInfoManager.INSTANCE.getPlayerGraves().values().forEach(graves -> graves.forEach(grave -> {
                if (anchor(grave) != null && grave.getStatus() != GraveStatus.CLAIMED) TRACKED.put(grave.getGraveId(), grave);
            }));
            event.getServer().getAllLevels().forEach(ShipGraves::observe);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> { TRACKED.clear(); PENDING_NOTICES.clear(); OBSERVED.clear(); });
    }

    public static RecoveryAnchor anchor(GraveComponent grave) {
        return ((GraveRecoveryState)(Object)grave).skyweave$anchor();
    }

    private static void anchor(GraveComponent grave, RecoveryAnchor anchor) {
        ((GraveRecoveryState)(Object)grave).skyweave$anchor(anchor);
        DeathInfoManager.INSTANCE.setDirty();
    }

    private static void observe(ServerLevel level) {
        if (!OBSERVED.add(level)) return;
        SubLevelContainer.getContainer(level).addObserver(new SubLevelObserver() {
            @Override public void onSubLevelRemoved(SubLevel ship, SubLevelRemovalReason reason) {
                for (var grave : TRACKED.values()) {
                    var link = anchor(grave);
                    if (link == null || !ship.getUniqueId().equals(link.construct())) continue;
                    Vec3 last = ship.logicalPose().transformPosition(plotPoint(ship, link.local()));
                    anchor(grave, new RecoveryAnchor(link.dimension(), link.construct(), link.local(), last,
                            reason == SubLevelRemovalReason.UNLOADED ? "unloaded" : "pending"));
                }
            }
        });
    }

    private static void sampleSupport(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        observe(player.serverLevel());
        var data = player.getPersistentData();
        RecoveryAnchor old = RecoveryAnchor.read(data.getCompound(SUPPORT));
        if (old != null && !old.dimension().equals(player.level().dimension().location().toString())) data.remove(SUPPORT);
        if (!player.onGround()) return;
        SubLevel ship = Sable.HELPER.getTrackingSubLevel(player);
        if (!(ship instanceof ServerSubLevel) || ship.isRemoved()) { data.remove(SUPPORT); return; }
        Vec3 local = ship.logicalPose().transformPositionInverse(player.position());
        BlockPos support = BlockPos.containing(local.add(0, -.1, 0));
        if (!loaded(player.serverLevel(), support, ship)
                || !player.level().getBlockState(support).isFaceSturdy(player.level(), support, Direction.UP)) return;
        BlockPos deck = support.above();
        var link = new RecoveryAnchor(player.level().dimension().location().toString(), ship.getUniqueId(),
                deck.subtract(ship.getPlot().getCenterBlock()), ship.logicalPose().transformPosition(deck.getCenter()), "support");
        data.put(SUPPORT, link.save());
    }

    private static void death(YigdEvents.DelayGraveGenerationEvent event) {
        if (event.generationIsDelayed()) return;
        var context = event.getDeathContext();
        var config = YigdConfig.getConfig();
        if (!config.graveConfig.enabled || !config.graveConfig.storeItems || !config.graveConfig.storeXp) return;
        var support = RecoveryAnchor.read(context.player().getPersistentData().getCompound(SUPPORT));
        if (support == null || !support.dimension().equals(context.world().dimension().location().toString())) return;
        context.player().getPersistentData().remove(SUPPORT);
        boolean voidDeath = context.deathSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                || context.deathPos().y < context.world().getMinBuildHeight();
        var ship = find(context.world(), support.construct());
        boolean onDeck = context.player().onGround() && ship != null
                && Sable.HELPER.getTrackingSubLevel(context.player()) == ship;
        if (!voidDeath && !onDeck) return;
        var grave = event.getGrave();
        if (grave.isGraveEmpty() && !config.graveConfig.generateEmptyGraves) return;
        var allow = NeoForge.EVENT_BUS.post(new YigdEvents.AllowGraveGenerationEvent(context, grave));
        anchor(grave, support.state(allow.isGenerationAllowed() ? "pending" : "blocked"));
        TRACKED.put(grave.getGraveId(), grave);
        event.setDelayGeneration(true);
        if (config.inventoryConfig.itemLoss.enabled) grave.getInventoryComponent().applyLoss();
        if (allow.isGenerationAllowed()) Yigd.END_OF_TICK.add(() -> place(grave, context.player(), event.getRespawnComponent()));
        else inform(context.player(), grave, "pending");
    }

    public static BlockPos worldPosition(GraveComponent grave) {
        return BlockPos.containing(worldCenter(grave));
    }

    public static Vec3 worldCenter(GraveComponent grave) {
        var link = anchor(grave);
        if (link == null) return grave.getPos().getCenter();
        var world = grave.getWorld();
        var ship = world == null ? null : find(world, link.construct());
        return ship == null ? link.fallback() : ship.logicalPose().transformPosition(plotPoint(ship, link.local()));
    }

    public static BlockPos closestGrave(UUID owner, ResourceKey<Level> dimension, BlockPos origin, BlockPos nativeResult) {
        boolean all = YigdConfig.getConfig().extraFeatures.graveCompass.pointToClosest
                == ExtraFeaturesConfig.GraveCompassConfig.CompassGraveTarget.ALL;
        BlockPos closest = nativeResult;
        for (var grave : TRACKED.values()) {
            if (grave.getWorldRegistryKey().equals(dimension) && grave.getPos().equals(nativeResult)) closest = null;
        }
        for (var grave : TRACKED.values()) {
            if (grave.getStatus() != GraveStatus.UNCLAIMED || !grave.getWorldRegistryKey().equals(dimension)
                    || (!all && !grave.getOwner().id().filter(owner::equals).isPresent())) continue;
            BlockPos position = worldPosition(grave);
            if (closest == null || origin.distSqr(position) < origin.distSqr(closest)) closest = position;
        }
        return closest;
    }

    public static boolean currentPhysicalGrave(GraveComponent grave, ServerLevel level, BlockPos pos) {
        var link = anchor(grave);
        if (link == null) return true;
        if (!link.dimension().equals(level.dimension().location().toString())) return false;
        if (grave.getWorld() != level || !grave.getPos().equals(pos)
                || !(link.state().equals("ship") || link.state().equals("world") || link.state().equals("unloaded"))) return false;
        SubLevel containing = Sable.HELPER.getContaining(level, pos);
        if (link.construct() == null) return containing == null;
        return containing != null && !containing.isRemoved() && containing.getUniqueId().equals(link.construct())
                && containing.getPlot().getCenterBlock().offset(link.local()).equals(pos);
    }

    /** Called before YiGD accepts a block entity's native relocation. */
    public static boolean acceptRelocation(GraveComponent grave, ServerLevel level, BlockPos pos) {
        var link = anchor(grave);
        if (link == null) return true;
        if (!link.dimension().equals(level.dimension().location().toString())) return false;
        if (currentPhysicalGrave(grave, level, pos)) return true;
        if (grave.getStatus() == GraveStatus.CLAIMED) return false;
        var oldShip = find(level, link.construct());
        if (oldShip != null && loaded(level, grave.getPos(), oldShip)
                && level.getBlockEntity(grave.getPos()) instanceof GraveBlockEntity original
                && grave.getGraveId().equals(original.getGraveId())) return false;
        SubLevel next = Sable.HELPER.getContaining(level, pos);
        if (next != null && !next.isRemoved() && next.getUniqueId().equals(link.construct())
                && next.getPlot().getCenterBlock().offset(link.local()).equals(pos)) {
            anchor(grave, new RecoveryAnchor(link.dimension(), next.getUniqueId(), link.local(),
                    next.logicalPose().transformPosition(pos.getCenter()), "ship"));
            return true;
        }
        if (oldShip == null && next == null && link.construct() != null
                && pos.getCenter().distanceToSqr(link.fallback()) <= 64
                && !link.state().equals("world")) {
            anchor(grave, new RecoveryAnchor(level.dimension().location().toString(), null, BlockPos.ZERO, pos.getCenter(), "world"));
            return true;
        }
        return false;
    }

    /** YiGD calls this after a block disappears, including during construct removal. */
    public static boolean preserveMissingGrave(GraveComponent grave) {
        var link = anchor(grave);
        if (link == null || grave.getStatus() == GraveStatus.CLAIMED) return false;
        var level = grave.getWorld();
        var ship = level == null ? null : find(level, link.construct());
        if (level != null && loaded(level, grave.getPos(), ship)
                && level.getBlockEntity(grave.getPos()) instanceof GraveBlockEntity be
                && grave.getGraveId().equals(be.getGraveId())) return true;
        if (!link.state().equals("unloaded") && !link.state().equals("blocked")) anchor(grave, link.state("pending"));
        return true;
    }

    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        for (var grave : List.copyOf(TRACKED.values())) {
            if (grave.getStatus() == GraveStatus.CLAIMED
                    || DeathInfoManager.INSTANCE.getGrave(grave.getGraveId()).orElse(null) != grave) {
                TRACKED.remove(grave.getGraveId()); PENDING_NOTICES.remove(grave.getGraveId()); continue;
            }
            var link = anchor(grave);
            var world = grave.getWorld();
            if (link == null || world == null) continue;
            var ship = find(world, link.construct());
            if (ship != null && (link.state().equals("ship") || link.state().equals("unloaded"))) {
                Vec3 point = ship.logicalPose().transformPosition(plotPoint(ship, link.local()));
                BlockPos physical = ship.getPlot().getCenterBlock().offset(link.local());
                if (loaded(world, physical, ship) && world.getBlockEntity(physical) instanceof GraveBlockEntity be
                        && grave.getGraveId().equals(be.getGraveId())) grave.setPos(physical);
                anchor(grave, new RecoveryAnchor(link.dimension(), link.construct(), link.local(), point, "ship"));
            } else if (link.state().equals("pending")) {
                var owner = grave.getOwner().id().map(event.getServer().getPlayerList()::getPlayer).orElse(null);
                if (owner != null) place(grave, owner, null);
            }
        }
    }

    private record Candidate(BlockPos physical, BlockPos projected, ServerSubLevel ship) { }

    private static void place(GraveComponent grave, ServerPlayer owner, RespawnComponent respawn) {
        var link = anchor(grave);
        var level = grave.getWorld();
        if (level == null || link == null || grave.getStatus() == GraveStatus.CLAIMED) return;
        var ship = find(level, link.construct());
        Candidate candidate = ship == null ? null : search(level, owner, BlockPos.containing(plotPoint(ship, link.local())), ship);
        if (candidate == null) candidate = search(level, owner, BlockPos.containing(link.fallback()), null);
        if (candidate == null) { anchor(grave, link.state("pending")); inform(owner, grave, "pending"); return; }
        BlockPos pos = candidate.physical();
        BlockState previous = level.getBlockState(pos);
        BlockPos oldPos = grave.getPos();
        grave.setPos(pos);
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && !NeoForge.EVENT_BUS.post(new YigdEvents.AllowBlockUnderGraveGenerationEvent(grave,
                        level.getBlockState(pos.below()))).isPlacementAllowed()) {
            grave.setPos(oldPos);
            anchor(grave, link.state("pending"));
            inform(owner, grave, "pending");
            return;
        }
        var state = Yigd.GRAVE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, owner.getDirection())
                .setValue(BlockStateProperties.WATERLOGGED, false);
        if (!grave.tryPlaceGrave(state) || !(level.getBlockEntity(pos) instanceof GraveBlockEntity be)) {
            grave.setPos(oldPos);
            anchor(grave, link.state("pending"));
            inform(owner, grave, "pending");
            return;
        }
        be.setPreviousState(previous);
        be.setComponent(grave);
        grave.setStatus(GraveStatus.UNCLAIMED);
        PENDING_NOTICES.remove(grave.getGraveId());
        if (candidate.ship() == null) {
            anchor(grave, new RecoveryAnchor(link.dimension(), null, BlockPos.ZERO, pos.getCenter(), "world"));
        } else {
            anchor(grave, new RecoveryAnchor(link.dimension(), candidate.ship().getUniqueId(),
                    pos.subtract(candidate.ship().getPlot().getCenterBlock()), candidate.projected().getCenter(), "ship"));
        }
        SubLevel oldShip = Sable.HELPER.getContaining(level, oldPos);
        if (!oldPos.equals(pos) && loaded(level, oldPos, oldShip)
                && level.getBlockEntity(oldPos) instanceof GraveBlockEntity old
                && grave.getGraveId().equals(old.getGraveId())) level.removeBlock(oldPos, false);
        if (respawn != null) respawn.setGraveGenerated(true);
        if (respawn == null) inform(owner, grave, "placed");
    }

    private static Candidate search(ServerLevel level, ServerPlayer owner, BlockPos origin, ServerSubLevel ship) {
        for (var delta : RecoverySearch.OFFSETS) {
            BlockPos pos = origin.offset(delta.x(), delta.y(), delta.z());
            if (!loaded(level, pos, ship) || !level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.below())) continue;
            BlockPos projected = ship == null ? pos : BlockPos.containing(ship.logicalPose().transformPosition(pos.getCenter()));
            BlockPos projectedSupport = ship == null ? pos.below() : BlockPos.containing(ship.logicalPose().transformPosition(pos.below().getCenter()));
            if (!level.getWorldBorder().isWithinBounds(projected) || !level.isInWorldBounds(projected)
                    || !level.getWorldBorder().isWithinBounds(projectedSupport) || !level.isInWorldBounds(projectedSupport)
                    || !level.mayInteract(owner, projected) || !level.mayInteract(owner, pos)
                    || !level.mayInteract(owner, projectedSupport) || !level.mayInteract(owner, pos.below())) continue;
            if (!level.getBlockState(pos).isAir() || level.getBlockEntity(pos) != null) continue;
            var below = level.getBlockState(pos.below());
            boolean support = below.isFaceSturdy(level, pos.below(), Direction.UP);
            boolean maySupport = ship == null && below.isAir() && level.getBlockEntity(pos.below()) == null
                    && YigdConfig.getConfig().graveConfig.blockUnderGrave.enabled;
            if (!support && !maySupport) continue;
            if (below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) continue;
            var event = NeoForge.EVENT_BUS.post(new RecoveryPlacementEvent(owner, level, pos, projected));
            if (!event.isCanceled()) return new Candidate(pos, projected, ship);
        }
        return null;
    }

    private static boolean loaded(ServerLevel level, BlockPos pos, SubLevel ship) {
        return ship == null ? !SubLevelContainer.getContainer(level).inBounds(pos) && level.hasChunkAt(pos)
                : ship.getPlot().contains(new net.minecraft.world.level.ChunkPos(pos))
                    && ship.getPlot().getChunk(ship.getPlot().toLocal(new net.minecraft.world.level.ChunkPos(pos))) != null;
    }

    private static ServerSubLevel find(ServerLevel level, UUID id) {
        if (id == null) return null;
        var ship = SubLevelContainer.getContainer(level).getSubLevel(id);
        return ship instanceof ServerSubLevel serverShip && !ship.isRemoved() ? serverShip : null;
    }

    private static Vec3 plotPoint(SubLevel ship, BlockPos relative) {
        return ship.getPlot().getCenterBlock().offset(relative).getCenter();
    }

    private static void inform(ServerPlayer owner, GraveComponent grave, String state) {
        if (state.equals("pending") && !PENDING_NOTICES.add(grave.getGraveId())) return;
        BlockPos pos = worldPosition(grave);
        owner.sendSystemMessage(Component.translatable("message.skyweave_compat.recovery." + state,
                pos.toShortString(), grave.getWorldRegistryKey().location().toString(), grave.getGraveId().toString()));
    }
}

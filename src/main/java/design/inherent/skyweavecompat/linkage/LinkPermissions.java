package design.inherent.skyweavecompat.linkage;

import com.enxv.aerouniversaljoint.content.*;
import com.enxv.aerouniversaljoint.network.*;
import com.enxv.aerouniversaljoint.util.SubLevelReferenceHelper;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Server-thread checks for player mutations. Physics and lifecycle cleanup do not use this policy. */
public final class LinkPermissions {
    private LinkPermissions() {}

    public static boolean removalGesture(Player player) {
        return player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof WrenchItem;
    }

    public static boolean canModify(Player player, BlockEntity entity, boolean removing) {
        return canModify(player, entity, removing, false);
    }

    public static boolean canRemoveBlock(Player player, BlockEntity entity) {
        // Upstream WrenchRemovalHelper posts its own plot-position BreakEvent after this guard.
        return canModify(player, entity, true, true);
    }

    private static boolean canModify(Player player, BlockEntity entity, boolean removing, boolean upstreamBreak) {
        if (!(player instanceof ServerPlayer server) || !player.mayBuild() || player.isSpectator()
                || entity == null || entity.isRemoved() || entity.getLevel() != player.level()
                || !player.level().hasChunkAt(entity.getBlockPos())
                || player.level().getBlockEntity(entity.getBlockPos()) != entity) return false;
        if (!endpoint(server, entity, removing, upstreamBreak)) return false;
        BlockPos linked = null;
        java.util.UUID subLevel = null;
        if (entity instanceof HydraulicConnectionHeadBlockEntity head && head.hasLink()) {
            linked = head.getLinkedPos(); subLevel = head.getLinkedSubLevelId();
        } else if (entity instanceof UniversalJointBlockEntity joint && joint.hasLink()) {
            linked = joint.getLinkedPos(); subLevel = joint.getLinkedSubLevelId();
        }
        if (linked == null) return true;
        var peer = SubLevelReferenceHelper.resolveBlockEntityFast(player.level(), linked, subLevel);
        return peer != null && !peer.isRemoved() && endpoint(server, peer, removing, false);
    }

    private static boolean endpoint(ServerPlayer player, BlockEntity entity, boolean removing, boolean upstreamBreak) {
        var level = player.serverLevel();
        var plot = entity.getBlockPos();
        var world = BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, plot.getCenter()));
        // Plot-aware integrations and ordinary spawn protection see their respective coordinates.
        return position(player, plot, removing && !upstreamBreak)
                && (plot.equals(world) || position(player, world, removing))
                && !NeoForge.EVENT_BUS.post(new LinkageEditEvent(player, entity, world, removing)).isCanceled();
    }

    private static boolean position(ServerPlayer player, BlockPos pos, boolean removing) {
        var level = player.serverLevel();
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem()))
            return false;
        if (removing) {
            return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player)).isCanceled();
        }
        return true;
    }

    public static boolean packet(Player player, Object payload) {
        BlockEntity target;
        boolean removing = payload instanceof BreakHydraulicRodPayload;
        if (payload instanceof BreakHydraulicRodPayload p) {
            if (!removalGesture(player)) return false;
            target = SubLevelReferenceHelper.resolveBlockEntityFast(player.level(), p.pos(), p.subLevelId());
        } else if (payload instanceof OpenHydraulicRodSettingsPayload p) {
            target = SubLevelReferenceHelper.resolveBlockEntityFast(player.level(), p.pos(), p.subLevelId());
        } else {
            if (!player.containerMenu.stillValid(player)) return false;
            BlockPos pos;
            if (payload instanceof SetHydraulicSettingsPayload p) pos = p.pos();
            else if (payload instanceof SetGiantHydraulicSettingsPayload p) pos = p.pos();
            else if (payload instanceof SetHydraulicHingeLimitsPayload p) pos = p.pos();
            else if (payload instanceof SetDampingResistancePayload p) pos = p.pos();
            else if (payload instanceof SetJointSpeedRatioPayload p) pos = p.pos();
            else return false;
            target = player.containerMenu instanceof HydraulicConnectionHeadMenu menu
                    ? menu.getBlockEntity() : player.level().hasChunkAt(pos) ? player.level().getBlockEntity(pos) : null;
            if (target == null || !target.getBlockPos().equals(pos)) return false;
        }
        return canModify(player, target, removing);
    }
}

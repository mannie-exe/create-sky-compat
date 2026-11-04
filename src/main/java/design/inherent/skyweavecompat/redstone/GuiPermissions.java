package design.inherent.skyweavecompat.redstone;

import com.ggrgg.createredstonelinkgui.common.VoidLinkHelper;
import com.ggrgg.createredstonelinkgui.common.menu.AbstractLinkMenu;
import com.ggrgg.createredstonelinkgui.common.network.*;
import com.ggrgg.createredstonelinkgui.common.preset.FrequencyPresetData;
import com.ggrgg.createredstonelinkgui.compat.propulsion.VectorThrusterHelper;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import design.inherent.skyweavecompat.Feature;
import design.inherent.skyweavecompat.OptionalIntegrations;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/** Common server authorization used by both payloads and native menu clicks. */
public final class GuiPermissions {
    public static Vec3 project(Level level, BlockPos pos) {
        return OptionalIntegrations.enabled(Feature.LINK_GUI_SABLE) ? GuiShipAccess.project(level, pos) : pos.getCenter();
    }
    public static UUID frame(Level level, BlockPos pos) {
        return OptionalIntegrations.enabled(Feature.LINK_GUI_SABLE) ? GuiShipAccess.frame(level, pos) : null;
    }
    public static boolean position(ServerPlayer player, BlockEntity source, BlockPos pos, boolean moving) {
        var level = player.serverLevel();
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)) return false;
        BlockPos world = BlockPos.containing(project(level, pos));
        return level.isInWorldBounds(world) && level.getWorldBorder().isWithinBounds(world)
                && level.mayInteract(player, pos) && level.mayInteract(player, world)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem())
                && player.mayUseItemAt(world, Direction.UP, player.getMainHandItem())
                && !NeoForge.EVENT_BUS.post(new RedstoneEditEvent(player, source, pos, world, moving)).isCanceled();
    }
    public static boolean canEdit(Player player, BlockPos pos, BlockEntity expected, boolean moving, double range) {
        if (!(player instanceof ServerPlayer server) || !player.mayBuild() || player.isSpectator()
                || expected == null || expected.isRemoved() || expected.getLevel() != player.level()
                || !player.level().hasChunkAt(pos) || player.level().getBlockEntity(pos) != expected
                || player.position().distanceToSqr(project(player.level(), pos)) > range * range) return false;
        Object voidLink = VoidLinkHelper.getBehaviour(player.level(), pos);
        return (voidLink == null || VoidLinkHelper.canInteract(voidLink, player)) && position(server, expected, pos, moving);
    }
    public static boolean menuValid(AbstractLinkMenu menu, Player player) {
        if (player.level().isClientSide) return true;
        var captured = (GuiMenuState) menu;
        return captured.skyweave$owner() == player
                && Objects.equals(captured.skyweave$frame(), frame(player.level(), menu.getPos()))
                && canEdit(player, menu.getPos(), captured.skyweave$initialTarget(), false, 8);
    }
    private static boolean menu(Player player, BlockPos pos, int cell, String side) {
        return player.containerMenu instanceof AbstractLinkMenu menu && menu.getPos().equals(pos)
                && menu.getCellIndex() == cell && Objects.equals(menu.getSideKey(), side) && menuValid(menu, player);
    }
    private static boolean preset(int index) { return index >= 0 && index < FrequencyPresetData.PRESET_COUNT; }
    private static boolean slot(int index) { return index >= 0 && index < 2; }
    public static boolean packet(Player player, Object payload) {
        if (payload instanceof PresetSlotUpdatePayload p)
            return preset(p.presetIndex()) && slot(p.slotIndex()) && player.containerMenu instanceof AbstractLinkMenu menu && menuValid(menu, player);
        BlockPos pos = switch (payload) {
            case OpenLinkMenuPayload p -> p.pos(); case RedstoneLinkFrequencyPayload p -> p.pos();
            case VectorThrusterFrequencyPayload p -> p.pos(); case RedstoneLinkModeTogglePayload p -> p.pos();
            case CopyToPresetPayload p -> p.pos(); case PasteFromPresetPayload p -> p.pos();
            case VoidLinkClaimPayload p -> p.pos(); case TinyLinkFreqUpdatePayload p -> p.pos();
            case TinyLinkScreenSwapPayload p -> p.pos(); default -> null;
        };
        if (pos == null || !player.level().hasChunkAt(pos)) return false;
        var target = player.level().getBlockEntity(pos);
        if (!canEdit(player, pos, target, false, 8)) return false;
        if (payload instanceof OpenLinkMenuPayload p) {
            if (p.sideKey() == null) return false;
            if (!p.sideKey().isEmpty()) return VectorThrusterHelper.getBehaviour(player.level(), pos, p.sideKey()) != null;
            return BlockEntityBehaviour.get(target, LinkBehaviour.TYPE) != null || VoidLinkHelper.getBehaviour(player.level(), pos) != null;
        }
        if (payload instanceof TinyLinkScreenSwapPayload p)
            return p.cellIndex() >= 0 && com.ggrgg.createredstonelinkgui.common.TinyRedstoneCreateCompatibility.findCell(player.level(), pos, p.cellIndex()) != null;
        if (payload instanceof RedstoneLinkFrequencyPayload p) return slot(p.slotIndex()) && menu(player, pos, -1, "");
        if (payload instanceof VectorThrusterFrequencyPayload p) return slot(p.slotIndex()) && menu(player, pos, -1, p.sideKey());
        if (payload instanceof TinyLinkFreqUpdatePayload p) return menu(player, pos, p.cellIndex(), "");
        if (payload instanceof CopyToPresetPayload p) return preset(p.presetIndex()) && menu(player, pos, p.cellIndex(), p.sideKey());
        if (payload instanceof PasteFromPresetPayload p) return preset(p.presetIndex()) && menu(player, pos, p.cellIndex(), p.sideKey());
        return menu(player, pos, -1, "");
    }
    private GuiPermissions() {}
}

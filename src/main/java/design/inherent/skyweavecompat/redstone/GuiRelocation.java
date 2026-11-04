package design.inherent.skyweavecompat.redstone;

import com.ggrgg.createredstonelinkgui.Config;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class GuiRelocation {
    public static boolean movable(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(ResourceLocation.parse("create:redstone_link"))) return false;
        var support = BlockEntityBehaviour.get(level, pos, FactoryPanelSupportBehaviour.TYPE);
        return support == null || support.getLinkedPanels().isEmpty();
    }
    public static boolean allow(Player player, RedstoneLinkMovePayload payload) {
        var level = player.level(); var source = payload.sourcePos(); var target = payload.targetPos();
        if (!(player instanceof ServerPlayer server) || !movable(level, source) || !level.hasChunkAt(target)) return deny(player);
        var entity = level.getBlockEntity(source); double range = Config.MOVE_RANGE.get();
        if (!GuiPermissions.canEdit(player, source, entity, true, range)
                || !Objects.equals(GuiPermissions.frame(level, source), GuiPermissions.frame(level, target))
                || GuiPermissions.project(level, source).distanceToSqr(GuiPermissions.project(level, target)) > range * range
                || !GuiPermissions.position(server, entity, target, true)
                || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, source, level.getBlockState(source), player)).isCanceled()) return deny(player);
        // The stock handler retains orientation, support, replaceability and distance checks.
        return true;
    }
    private static boolean deny(Player player) {
        player.displayClientMessage(Component.translatable("message.skyweave_compat.redstone.move_denied"), true); return false;
    }
    private GuiRelocation() {}
}

package design.inherent.createskycompat.clientprobe;

import java.util.List;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.infernalstudios.questlog.core.ServerPlayerManager;
import org.infernalstudios.questlog.network.IPacketContext;
import org.infernalstudios.questlog.network.packet.QuestRewardCollectPacket;

/** Actual upstream handler, invoked on the fixture server thread before gameplay. */
final class JournalClaimChecks {
    private JournalClaimChecks() {}
    static JsonObject run(net.minecraft.server.level.ServerPlayer player) {
        var manager = ServerPlayerManager.INSTANCE.getManagerByPlayer(player);
        var id = ResourceLocation.parse("questlog:ux/claims");
        var quest = manager.getQuest(id);
        if (quest == null) throw new IllegalStateException("Missing isolated journal fixture");
        var context = new IPacketContext() {
            @Override public net.minecraft.world.entity.player.Player getSender() { return player; }
            @Override public Direction getDirection() { return Direction.CLIENT_TO_SERVER; }
        };
        var result = new JsonObject();
        quest.resetProgress(); player.getInventory().clearContent();
        QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 0, List.of()), context);
        result.addProperty("incompleteRejected", !quest.rewards.getFirst().hasRewarded());
        boolean boundsSafe = true;
        try {
            QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, -1, List.of()), context);
            QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 999, List.of()), context);
        } catch (RuntimeException failure) { boundsSafe = false; }
        result.addProperty("boundsRejected", boundsSafe);
        quest.objectives.getFirst().forceSetUnits(1);
        QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 1, List.of(0, 0)), context);
        result.addProperty("duplicateChoicesRejected", !quest.rewards.get(1).hasRewarded());
        QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 1, List.of(2)), context);
        result.addProperty("invalidChoiceRejected", !quest.rewards.get(1).hasRewarded());
        QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 0, List.of()), context);
        int before = player.getInventory().countItem(Items.BREAD);
        QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 0, List.of()), context);
        result.addProperty("singleGrant", before == 2 && player.getInventory().countItem(Items.BREAD) == 2);
        QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 1, List.of(1)), context);
        result.addProperty("validChoice", quest.rewards.get(1).hasRewarded() && player.getInventory().countItem(Items.CARROT) == 1);
        var gameType = player.gameMode.getGameModeForPlayer();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new net.minecraft.world.item.ItemStack(Items.STONE, 64));
        var area = player.getBoundingBox().inflate(8);
        java.util.function.IntSupplier droppedBread = () -> player.serverLevel()
                .getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)
                .stream().filter(item -> item.getItem().is(Items.BREAD)).mapToInt(item -> item.getItem().getCount()).sum();
        int droppedBefore = droppedBread.getAsInt();
        quest.rewards.getFirst().revokeReward();
        QuestRewardCollectPacket.handle(new QuestRewardCollectPacket(id, 0, List.of()), context);
        result.addProperty("survivalOverflowDropsRemainder", player.getInventory().countItem(Items.BREAD) == 0
                && droppedBread.getAsInt() - droppedBefore == 2 && quest.rewards.getFirst().hasRewarded());
        player.getInventory().clearContent(); player.setGameMode(gameType);
        return result;
    }
}

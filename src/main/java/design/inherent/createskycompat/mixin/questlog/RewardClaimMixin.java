package design.inherent.createskycompat.mixin.questlog;

import design.inherent.createskycompat.questlog.ClaimRules;
import org.infernalstudios.questlog.core.ServerPlayerManager;
import org.infernalstudios.questlog.core.quests.rewards.ChoiceReward;
import org.infernalstudios.questlog.network.IPacketContext;
import org.infernalstudios.questlog.network.packet.QuestRewardCollectPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retain Questlog's grant/save path, but reject invalid requests before any mutation. */
@Mixin(value = QuestRewardCollectPacket.class, remap = false)
public abstract class RewardClaimMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void createSky$validate(QuestRewardCollectPacket packet, IPacketContext context, CallbackInfo ci) {
        var sender = context.getSender();
        if (sender == null) { ci.cancel(); return; }
        var manager = ServerPlayerManager.INSTANCE.getManagerByPlayer(sender);
        var quest = manager.getQuest(packet.id());
        int index = packet.rewardIndex();
        if (quest == null || index < 0 || index >= quest.rewards.size()) { ci.cancel(); return; }
        var reward = quest.rewards.get(index);
        if (!ClaimRules.eligible(quest.isTriggered(), quest.isCompleted(), reward.hasRewarded(), index, quest.rewards.size())) {
            ci.cancel(); return;
        }
        boolean valid = reward instanceof ChoiceReward choice
                ? ClaimRules.selections(packet.selections(), choice.getChoices().size(), choice.getPickCount())
                : ClaimRules.selections(packet.selections(), 0, 0);
        if (!valid) ci.cancel();
    }
}

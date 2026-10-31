package design.inherent.createskycompat.mixin.client;

import design.inherent.createskycompat.questlog.client.JournalNotifications;
import org.infernalstudios.questlog.QuestlogClientEvents;
import org.infernalstudios.questlog.event.events.QuestEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuestlogClientEvents.class, remap = false)
public abstract class QuestNotificationMixin {
    @Inject(method = "onQuestTriggered", at = @At("HEAD"), cancellable = true, require = 1)
    private static void createSky$noUnlockPopup(QuestEvent.Triggered event, CallbackInfo ci) { ci.cancel(); }
    @Inject(method = "onQuestCompleted", at = @At("HEAD"), cancellable = true, require = 1)
    private static void createSky$complete(QuestEvent.Completed event, CallbackInfo ci) {
        JournalNotifications.complete(event.quest); ci.cancel();
    }
    @Inject(method = "onClientTick", at = @At("TAIL"), require = 1)
    private static void createSky$tick(CallbackInfo ci) { JournalNotifications.tick(); }
    @Inject(method = "onClientPlayerLogout", at = @At("HEAD"), require = 1)
    private static void createSky$logout(CallbackInfo ci) { JournalNotifications.clear(); }
}

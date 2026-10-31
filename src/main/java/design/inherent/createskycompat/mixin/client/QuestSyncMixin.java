package design.inherent.createskycompat.mixin.client;

import design.inherent.createskycompat.questlog.client.JournalNotifications;
import org.infernalstudios.questlog.network.ClientPacketHandler;
import org.infernalstudios.questlog.network.IPacketContext;
import org.infernalstudios.questlog.network.packet.QuestDataPacket;
import org.infernalstudios.questlog.network.packet.QuestRemovePacket;
import org.infernalstudios.questlog.network.packet.QuestSyncPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketHandler.class, remap = false)
public abstract class QuestSyncMixin {
    @Inject(method = "processSync", at = @At("TAIL"), require = 1)
    private static void createSky$baseline(QuestSyncPacket packet, CallbackInfo ci) { JournalNotifications.baseline(); }
    @Inject(method = "handle(Lorg/infernalstudios/questlog/network/packet/QuestDataPacket;Lorg/infernalstudios/questlog/network/IPacketContext;)V",
            at = @At("TAIL"), require = 1)
    private static void createSky$progress(QuestDataPacket packet, IPacketContext context, CallbackInfo ci) { JournalNotifications.update(packet.id()); }
    @Inject(method = "handle(Lorg/infernalstudios/questlog/network/packet/QuestRemovePacket;Lorg/infernalstudios/questlog/network/IPacketContext;)V",
            at = @At("TAIL"), require = 1)
    private static void createSky$remove(QuestRemovePacket packet, IPacketContext context, CallbackInfo ci) { JournalNotifications.remove(packet.id()); }
}

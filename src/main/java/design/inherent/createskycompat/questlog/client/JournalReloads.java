package design.inherent.createskycompat.questlog.client;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Resource/font reloads invalidate open-screen measurements, not quest state. */
public final class JournalReloads {
    private static volatile long revision;
    private JournalReloads() {}
    public static long revision() { return revision; }
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> revision++);
    }
}

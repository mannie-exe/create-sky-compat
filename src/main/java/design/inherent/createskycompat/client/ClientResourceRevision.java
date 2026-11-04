package design.inherent.createskycompat.client;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Resource/font reloads invalidate open-screen measurements, not gameplay state. */
public final class ClientResourceRevision {
    private static volatile long revision;
    private ClientResourceRevision() {}
    public static long revision() { return revision; }
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> revision++);
    }
}

package design.inherent.createskycompat.clientprobe;

import java.nio.file.Files;
import com.google.gson.GsonBuilder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** The same opt-in claim fixture on a physical dedicated server, with no client classes. */
@Mod(value = "create_sky_client_probe", dist = Dist.DEDICATED_SERVER)
public final class DedicatedJournalProbe {
    private boolean checked;
    public DedicatedJournalProbe() {
        if (Boolean.getBoolean("create_sky_compat.probe.journal")) NeoForge.EVENT_BUS.addListener(this::tick);
    }
    private void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (checked || server.getPlayerCount() == 0 || server.getTickCount() < 60) return;
        checked = true;
        var result = JournalClaimChecks.run(server.getPlayerList().getPlayers().getFirst());
        try { Files.writeString(FMLPaths.GAMEDIR.get().resolve("journal-claims.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); }
        catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
}

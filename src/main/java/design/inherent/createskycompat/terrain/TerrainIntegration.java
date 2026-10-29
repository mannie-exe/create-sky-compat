package design.inherent.createskycompat.terrain;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Loaded only when the pinned Sky Archipelago preset adaptation is active. */
public final class TerrainIntegration {
    private TerrainIntegration() {}

    public static void register() {
        var directory = FMLPaths.CONFIGDIR.get();
        GenerationConfiguration.load(directory);
        InteriorConfiguration.load(directory);
        WaterConfiguration.load(directory);
        OceanConfiguration.load(directory);
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> GenerationGuard.verify(event.getServer()));
    }
}

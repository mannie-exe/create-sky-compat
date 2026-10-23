package design.inherent.skyvillageneighbors;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(SkyVillageNeighbors.ID)
public final class SkyVillageNeighbors {
    public static final String ID = "sky_village_neighbors";

    public SkyVillageNeighbors(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, NeighborConfig.SPEC);
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> NeighborConfig.beginServer());
    }
}

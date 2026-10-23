package design.inherent.skyvillageneighbors.client;

import design.inherent.skyvillageneighbors.SkyVillageNeighbors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = SkyVillageNeighbors.ID, dist = Dist.CLIENT)
public final class ClientConfiguration {
    public ClientConfiguration(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}

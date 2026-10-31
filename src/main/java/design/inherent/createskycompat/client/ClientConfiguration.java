package design.inherent.createskycompat.client;

import design.inherent.createskycompat.CreateSkyCompat;
import design.inherent.createskycompat.Feature;
import design.inherent.createskycompat.OptionalIntegrations;
import design.inherent.createskycompat.questlog.client.JournalPreferences;
import design.inherent.createskycompat.questlog.client.JournalReloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateSkyCompat.ID, dist = Dist.CLIENT)
public final class ClientConfiguration {
    public ClientConfiguration(ModContainer container, IEventBus modBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationOverview::new);
        if (OptionalIntegrations.enabled(Feature.QUESTLOG_INTERFACE)
                || OptionalIntegrations.enabled(Feature.QUESTLOG_NOTIFICATIONS)) {
            container.registerConfig(ModConfig.Type.CLIENT, JournalPreferences.SPEC,
                    "create_sky_compat-journal-client.toml");
            modBus.addListener(JournalReloads::register);
        }
    }
}

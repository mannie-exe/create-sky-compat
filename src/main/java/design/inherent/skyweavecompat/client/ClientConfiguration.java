package design.inherent.skyweavecompat.client;

import design.inherent.skyweavecompat.SkyweaveCompat;
import design.inherent.skyweavecompat.Feature;
import design.inherent.skyweavecompat.OptionalIntegrations;
import design.inherent.skyweavecompat.questlog.client.JournalPreferences;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = SkyweaveCompat.ID, dist = Dist.CLIENT)
public final class ClientConfiguration {
    public ClientConfiguration(ModContainer container, IEventBus modBus) {
        modBus.addListener(ClientResourceRevision::register);
        if (OptionalIntegrations.enabled(Feature.CREATE_BACKFACES))
            modBus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, PlainGlassModels::wrap);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationOverview::new);
        if (OptionalIntegrations.enabled(Feature.REDSTONE_RENDERING))
            modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() ->
                    net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                            net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create_redstone_additions:inverter")),
                            net.minecraft.client.renderer.RenderType.cutout())));
        if (OptionalIntegrations.enabled(Feature.LINEAR_PONDER))
            design.inherent.skyweavecompat.linear.client.LinearPonder.register();
        if (OptionalIntegrations.enabled(Feature.QUESTLOG_INTERFACE)
                || OptionalIntegrations.enabled(Feature.QUESTLOG_NOTIFICATIONS)) {
            container.registerConfig(ModConfig.Type.CLIENT, JournalPreferences.SPEC,
                    "skyweave_compat-journal-client.toml");
        }
    }
}

package design.inherent.createskycompat;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import design.inherent.createskycompat.placement.DeliveryChunks;
import net.neoforged.bus.api.IEventBus;

@Mod(CreateSkyCompat.ID)
public final class CreateSkyCompat {
    public static final String ID = "create_sky_compat";

    public CreateSkyCompat(ModContainer container, IEventBus modBus) {
        var selection = OptionalIntegrations.selection();
        com.mojang.logging.LogUtils.getLogger().info("Create Sky Compat active adaptations: {}", selection.active());
        com.mojang.logging.LogUtils.getLogger().debug("Create Sky Compat inactive adaptations: {}", selection.inactive());
        if (OptionalIntegrations.skyVillages()) {
            LegacyConfiguration.migrate(FMLPaths.CONFIGDIR.get());
            container.registerConfig(ModConfig.Type.COMMON, NeighborConfig.SPEC);
            NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> NeighborConfig.beginServer());
        }
        if (selection.enabled(Feature.TERRAIN)) {
            design.inherent.createskycompat.terrain.TerrainIntegration.register();
        } else {
            // This checks Compat's own save marker without loading or modifying any optional mod.
            NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> {
                var root = event.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                if (java.nio.file.Files.exists(root.resolve("create-sky-generation.json")))
                    throw new IllegalStateException("This save requires Create Sky Compat terrain.preset and Sky Archipelago 1.3.6. "
                            + "Restore its recorded configuration; automatic world conversion is unsupported.");
            });
        }
        if (selection.enabled(Feature.TERRAIN)) modBus.addListener(IntegrationPacks::register);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer() && OptionalIntegrations.blueMap()) {
            design.inherent.createskycompat.bluemap.BlueMapIntegration.register();
        }
        if (selection.enabled(Feature.DELIVERY_CHUNKS))
            NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> DeliveryChunks.stop(event.getServer()));
    }
}

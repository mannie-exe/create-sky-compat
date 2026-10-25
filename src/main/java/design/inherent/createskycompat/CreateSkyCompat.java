package design.inherent.createskycompat;

import design.inherent.createskycompat.terrain.GenerationGuard;
import design.inherent.createskycompat.terrain.GenerationConfiguration;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import design.inherent.createskycompat.placement.DeliveryChunks;

@Mod(CreateSkyCompat.ID)
public final class CreateSkyCompat {
    public static final String ID = "create_sky_compat";

    public CreateSkyCompat(ModContainer container) {
        GenerationConfiguration.load(FMLPaths.CONFIGDIR.get());
        LegacyConfiguration.migrate(FMLPaths.CONFIGDIR.get());
        container.registerConfig(ModConfig.Type.COMMON, NeighborConfig.SPEC);
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> {
            NeighborConfig.beginServer();
            GenerationGuard.verify(event.getServer());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> DeliveryChunks.stop(event.getServer()));
    }
}

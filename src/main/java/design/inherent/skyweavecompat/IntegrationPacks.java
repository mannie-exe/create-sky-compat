package design.inherent.skyweavecompat;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/** Inactive integrations expose no data, including registry entries and tags. */
final class IntegrationPacks {
    private IntegrationPacks() {}

    static void register(AddPackFindersEvent event) {
        add(event, Feature.TERRAIN, "terrain", "Skyweave island preset");
        add(event, Feature.ORE_PLACEMENT, "ores", "Skyweave relative feature placement");
        add(event, Feature.PRESSURE, "pressure", "Skyweave island pressure");
        add(event, Feature.LINEAR_BOOTSTRAP, "linear", "Linear Bearing block drops");
    }

    private static void add(AddPackFindersEvent event, Feature feature, String path, String title) {
        if (!OptionalIntegrations.enabled(feature)) return;
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(SkyweaveCompat.ID, "packs/" + path),
                PackType.SERVER_DATA, Component.literal(title), PackSource.BUILT_IN, true, Pack.Position.BOTTOM);
    }
}

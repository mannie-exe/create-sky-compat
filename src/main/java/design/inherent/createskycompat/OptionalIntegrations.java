package design.inherent.createskycompat;

import net.neoforged.fml.loading.LoadingModList;

/** Uses loader metadata only; safe before game and optional mod classes load. */
public final class OptionalIntegrations {
    private OptionalIntegrations() {}

    public static boolean loaded(String id) {
        return LoadingModList.get().getModFileById(id) != null;
    }

    public static boolean skyVillages() {
        return loaded("skyvillages") && loaded("architectury");
    }
}

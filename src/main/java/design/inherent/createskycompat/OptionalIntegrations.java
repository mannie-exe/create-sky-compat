package design.inherent.createskycompat;

import net.neoforged.fml.loading.LoadingModList;

/** Uses loader metadata only; safe before game and optional mod classes load. */
public final class OptionalIntegrations {
    private OptionalIntegrations() {}

    public static boolean loaded(String id) {
        return LoadingModList.get().getModFileById(id) != null;
    }

    public static boolean blueMap() {
        return version("bluemap", "5.7");
    }

    public static boolean distantHorizons() {
        return version("distanthorizons", "3.3.3");
    }

    public static boolean analogAudio() {
        return version("analogaudio", "0.1.5-hotfix.1") && version("create", "6.0.10");
    }

    private static boolean version(String id, String expected) {
        var file = LoadingModList.get().getModFileById(id);
        return file != null && file.getMods().stream().anyMatch(mod ->
                mod.getModId().equals(id) && mod.getVersion().toString().equals(expected));
    }

    public static boolean skyVillages() {
        return loaded("skyvillages") && loaded("architectury");
    }
}

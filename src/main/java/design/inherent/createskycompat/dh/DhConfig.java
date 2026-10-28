package design.inherent.createskycompat.dh;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DhConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SAVED_WORLD_IDENTITY;
    static {
        var builder = new ModConfigSpec.Builder();
        SAVED_WORLD_IDENTITY = builder.comment("Use a unique per-save DH client cache key for Sky Archipelago dimensions. Requires DH sendLevelKeys. Disabled by default to preserve existing caches. Enabling selects a new cache; it does not move or delete the old one. Once created, the save identity is retained even if this option is disabled.")
                .worldRestart().define("savedWorldIdentity", false);
        SPEC = builder.build();
    }
    private DhConfig() {}
    public static boolean savedWorldIdentity() { return SAVED_WORLD_IDENTITY.get(); }
}

package design.inherent.skyvillageneighbors;

import design.inherent.skyvillageneighbors.placement.NeighborRule;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeighborConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue MINIMUM_GAP;
    private static final ModConfigSpec.IntValue MAXIMUM_GAP;
    private static final ModConfigSpec.IntValue MINIMUM_ISLAND_RADIUS;
    private static volatile NeighborRule.Settings active = new NeighborRule.Settings(true, 64, 512, 96);

    static {
        var builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Filter new Sky Villages starts. Existing villages are unchanged.")
                .translation("sky_village_neighbors.config.enabled").worldRestart().define("enabled", true);
        MINIMUM_GAP = builder.comment("Minimum horizontal column distance from the full village bounds to any island terrain. Overlap is always rejected.")
                .translation("sky_village_neighbors.config.minimumGap").worldRestart().defineInRange("minimumGap", 64, 0, 2048);
        MAXIMUM_GAP = builder.comment("Maximum horizontal column distance to a qualifying island. Must be at least minimumGap.")
                .translation("sky_village_neighbors.config.maximumGap").worldRestart().defineInRange("maximumGap", 512, 0, 2048);
        MINIMUM_ISLAND_RADIUS = builder.comment("Minimum native descriptor radius of the nearby island. Does not change island generation.")
                .translation("sky_village_neighbors.config.minimumIslandRadius").worldRestart().defineInRange("minimumIslandRadius", 96, 0, 2048);
        SPEC = builder.build();
    }

    private NeighborConfig() {}

    public static NeighborRule.Settings settings() { return active; }

    public static void beginServer() {
        active = new NeighborRule.Settings(ENABLED.get(), MINIMUM_GAP.get(), MAXIMUM_GAP.get(), MINIMUM_ISLAND_RADIUS.get());
    }
}

package design.inherent.skyweavecompat.linear.client;

import design.inherent.skyweavecompat.SkyweaveCompat;
import design.inherent.skyweavecompat.Feature;
import design.inherent.skyweavecompat.OptionalIntegrations;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

/** Registered from the gated client constructor, before Ponder builds its index. */
public final class LinearPonder implements PonderPlugin {
    public static void register() { PonderIndex.addPlugin(new LinearPonder()); }
    @Override public String getModId() { return SkyweaveCompat.ID; }

    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        var guide = helper.forComponents(item("linear_bearing"), item("linear_casing"));
        guide.addStoryBoard("linear/workbench", LinearScenes::assembly);
        guide.addStoryBoard("linear/workbench", LinearScenes::brake);
        guide.addStoryBoard("linear/workbench", LinearScenes::recovery);
        if (OptionalIntegrations.enabled(Feature.LINEAR_TRANSMISSION)) {
            helper.forComponents(item("magnetic_port")).addStoryBoard("linear/workbench", LinearScenes::magnetic);
            helper.forComponents(item("torsional_anchor")).addStoryBoard("linear/workbench", LinearScenes::spring);
        }
        if (OptionalIntegrations.enabled(Feature.LINEAR_GYRO))
            helper.forComponents(item("gyroscopic_gearbox"), item("redstone_converter"))
                    .addStoryBoard("linear/workbench", LinearScenes::gyro);
    }

    private static ResourceLocation item(String path) {
        return ResourceLocation.fromNamespaceAndPath("linearbearing", path);
    }
}

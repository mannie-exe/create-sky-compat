package design.inherent.createskycompat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Select optional mixins before Mixin resolves their target classes. */
public final class OptionalMixinPlugin implements IMixinConfigPlugin {
    @Override public List<String> getMixins() {
        var mixins = new ArrayList<String>();
        if (OptionalIntegrations.distantHorizons()) {
            mixins.addAll(List.of("dh.WorldLevelKeyMixin", "dh.ChunkUnloadBudgetMixin"));
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
                mixins.addAll(List.of("dh.LegacyCacheMixin", "dh.ClientWrapperHandoffMixin"));
        }
        if (OptionalIntegrations.skyVillages()) mixins.addAll(List.of(
                "StructureSupportMixin", "VillageHeightMixin", "PlacementSaltAccessor",
                "VillageStructureStateMixin", "GeneratorStructureStateMixin"));
        if (OptionalIntegrations.loaded("sable")) mixins.add("DimensionPhysicsMixin");
        if (OptionalIntegrations.loaded("aeronautics_delivery_quests")) mixins.add("DeliveryLandingMixin");
        return mixins;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

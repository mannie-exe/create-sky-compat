package design.inherent.skyvillageneighbors.mixin;

import com.mojang.logging.LogUtils;
import design.inherent.skyvillageneighbors.NeighborConfig;
import design.inherent.skyvillageneighbors.placement.IslandTerrain;
import design.inherent.skyvillageneighbors.placement.NeighborRule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.sathrek.sky_archipelago.worldgen.structure.StructureSupportValidator;
import org.sathrek.sky_archipelago.worldgen.structure.StructureSupportContext;
import org.sathrek.sky_archipelago.worldgen.structure.StructurePlacementCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StructureSupportValidator.class, remap = false)
public abstract class StructureSupportMixin {
    @Inject(method = "evaluatePlacement", at = @At("RETURN"), cancellable = true, require = 1)
    private void skyVillageNeighbors$filter(StructureSupportContext context, StructureStart start,
            StructurePlacementCategory category, CallbackInfoReturnable<StructureSupportValidator.SupportReport> callback) {
        var report = callback.getReturnValue();
        if (!report.accepted() || report.category() != StructurePlacementCategory.SKY
                || !context.structureId().equals(ResourceLocation.fromNamespaceAndPath("skyvillages", "skyvillage"))) return;
        var bounds = start.getBoundingBox();
        var footprint = new NeighborRule.Footprint(bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
        var decision = NeighborRule.evaluate(footprint, NeighborConfig.settings(), new IslandTerrain(context));
        if (decision == NeighborRule.Decision.TOO_CLOSE || decision == NeighborRule.Decision.NO_HOST) {
            LogUtils.getLogger().debug("Sky Village Neighbors rejected {}: {}", bounds, decision);
            callback.setReturnValue(report.withAccepted(false));
        }
    }
}

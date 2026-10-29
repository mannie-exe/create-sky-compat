package design.inherent.createskycompat.mixin;

import com.mojang.logging.LogUtils;
import design.inherent.createskycompat.NeighborConfig;
import design.inherent.createskycompat.placement.IslandTerrain;
import design.inherent.createskycompat.placement.NeighborRule;
import design.inherent.createskycompat.placement.VillageAnchors;
import design.inherent.createskycompat.placement.VillagePlan;
import design.inherent.createskycompat.terrain.ProfiledTerrain;
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
    private void createSky$filter(StructureSupportContext context, StructureStart start,
            StructurePlacementCategory category, CallbackInfoReturnable<StructureSupportValidator.SupportReport> callback) {
        var report = callback.getReturnValue();
        if (!report.accepted() || report.category() != StructurePlacementCategory.SKY
                || !context.structureId().equals(ResourceLocation.fromNamespaceAndPath("skyvillages", "skyvillage"))) return;
        var bounds = start.getBoundingBox();
        if ((Object)context.islandField() instanceof ProfiledTerrain terrain && terrain.createSky$profile() != null
                && design.inherent.createskycompat.OptionalIntegrations.enabled(design.inherent.createskycompat.Feature.VILLAGE_PLACEMENT)) {
            var anchors = (VillageAnchors)(Object)context.islandField();
            int sourceX = start.getChunkPos().getMiddleBlockX(), sourceZ = start.getChunkPos().getMiddleBlockZ();
            int centerX = (bounds.minX() + bounds.maxX()) / 2, centerZ = (bounds.minZ() + bounds.maxZ()) / 2;
            var sourceOwner = VillagePlan.owner(sourceX, sourceZ, anchors.createSky$anchorsNear(sourceX, sourceZ, context.settings()));
            var finalOwner = VillagePlan.owner(centerX, centerZ, anchors.createSky$anchorsNear(centerX, centerZ, context.settings()));
            if (sourceOwner == null || !sourceOwner.equals(finalOwner)) {
                callback.setReturnValue(report.withAccepted(false));
                return;
            }
        }
        var footprint = new NeighborRule.Footprint(bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
        var decision = NeighborRule.evaluate(footprint, NeighborConfig.settings(), new IslandTerrain(context));
        if (decision == NeighborRule.Decision.TOO_CLOSE || decision == NeighborRule.Decision.NO_HOST) {
            LogUtils.getLogger().debug("Create Sky Compat rejected {}: {}", bounds, decision);
            callback.setReturnValue(report.withAccepted(false));
        }
    }
}

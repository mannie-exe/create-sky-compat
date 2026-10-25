package design.inherent.createskycompat.probe;

import com.google.gson.*;
import design.inherent.createskycompat.placement.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import java.util.*;

final class VillagePlanProbe {
    private VillagePlanProbe() {}
    static JsonObject run(ServerLevel level, IslandField field, SkyIslandSettings settings, ArchipelagoVillagePlacement placement) {
        var anchors = new LinkedHashSet<VillagePlan.Anchor>();
        for (var preview : field.collectIslandPreviewsInRadius(0, 0, 12000, settings)) {
            if (preview.family() != IslandField.IslandFamily.ANCHOR_PLATEAU) continue;
            var owner = placement.owner(new VillagePlan.Site(preview.x() >> 4, preview.z() >> 4));
            if (owner != null) anchors.add(owner);
        }
        var selected = new HashSet<VillagePlan.Site>();
        var before = new LinkedHashMap<VillagePlan.Anchor, List<VillagePlan.Site>>();
        var rows = new JsonArray(); int[] targets = new int[4], realized = new int[4];
        for (var anchor : anchors) {
            int target = VillageDistribution.forAnchor(anchor.seed());
            var sites = placement.sites(anchor);
            if (sites.size() > target) throw new AssertionError("Village target exceeded");
            targets[target]++; realized[sites.size()]++; before.put(anchor, sites);
            for (var site : sites) {
                if (!anchor.equals(placement.owner(site))) throw new AssertionError("Site changed owner");
                if (!selected.add(site)) throw new AssertionError("Duplicate village site across anchors");
                if (!placement.isStructureChunk(level.getChunkSource().getGeneratorState(), site.chunkX(), site.chunkZ())) {
                    throw new AssertionError("Selected site is not a native placement candidate");
                }
            }
            var row = new JsonObject(); row.add("anchor", new Gson().toJsonTree(anchor)); row.addProperty("target", target);
            row.add("sites", new Gson().toJsonTree(sites)); rows.add(row);
        }
        var sky = (SkyIslandChunkGenerator)level.getChunkSource().getGenerator();
        var freshGenerator = sky.withSkyIslandSettings(sky.activeGeneratorSettings());
        var freshState = freshGenerator.createState(level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).asLookup(),
                level.getChunkSource().randomState(), level.getSeed());
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.parse("skyvillages:skyvillage"));
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).wrapAsHolder(structure);
        var freshPlacement = (ArchipelagoVillagePlacement)freshState.getPlacementsForStructure(holder).getFirst();
        var reversed = new ArrayList<>(anchors); Collections.reverse(reversed);
        for (var anchor : reversed) if (!before.get(anchor).equals(freshPlacement.sites(anchor))) throw new AssertionError("Fresh reverse query order changed village sites");
        var ordinary = (SkyIslandChunkGenerator)level.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                .get(ResourceLocation.parse("sky_archipelago:sky_islands")).createWorldDimensions().overworld();
        var ordinaryState = ordinary.createState(level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).asLookup(),
                level.getChunkSource().randomState(), level.getSeed());
        var sourcePlacement = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceLocation.parse("skyvillages:skyvillage")).placement();
        if (ordinaryState.getPlacementsForStructure(holder).getFirst() != sourcePlacement) throw new AssertionError("Ordinary placement changed");
        var result = new JsonObject(); result.addProperty("anchors", anchors.size()); result.add("targetHistogram", new Gson().toJsonTree(targets));
        result.add("plannedHistogram", new Gson().toJsonTree(realized)); result.add("rows", rows); result.addProperty("orderIndependent", true);
        return result;
    }
}

package design.inherent.createskycompat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency decisions use metadata only; disabled features impose no version constraints. */
public record FeatureSelection(Set<Feature> active, Map<Feature, String> inactive) {
    public FeatureSelection {
        active = Set.copyOf(active);
        inactive = Map.copyOf(inactive);
    }

    public boolean enabled(Feature feature) { return active.contains(feature); }

    public static FeatureSelection select(FeatureSettings settings, Map<String, String> mods, boolean client) {
        var active = EnumSet.noneOf(Feature.class);
        var inactive = new LinkedHashMap<Feature, String>();
        for (var feature : Feature.values()) {
            if (!settings.enabled(feature)) { inactive.put(feature, "disabled in configuration"); continue; }
            if (clientOnly(feature) && !client || feature == Feature.BLUEMAP && client) {
                inactive.put(feature, "not applicable on this physical side"); continue;
            }
            Feature prerequisite = prerequisite(feature);
            if (prerequisite != null && !active.contains(prerequisite)) {
                inactive.put(feature, "requires active " + prerequisite.key); continue;
            }
            var required = dependencies(feature);
            var missing = required.keySet().stream().filter(id -> !mods.containsKey(id)).sorted().toList();
            if (!missing.isEmpty()) { inactive.put(feature, "missing " + String.join(", ", missing)); continue; }
            for (var entry : required.entrySet()) {
                String version = mods.get(entry.getKey()), expected = entry.getValue();
                if (!(expected.endsWith(".") ? version.startsWith(expected) : version.equals(expected)))
                    throw new IllegalStateException("Create Sky Compat " + feature.key + " requires "
                            + entry.getKey() + " " + expected + "; found " + version
                            + ". Disable this adaptation in " + FeatureSettings.FILE + " or install the supported version.");
            }
            if (feature == Feature.FRAMEBUFFER && mods.containsKey("veil_iris_fix"))
                throw new IllegalStateException("Remove Veil Iris Fix: its framebuffer correction is included in Create Sky Compat. "
                        + "Alternatively disable " + feature.key + " in " + FeatureSettings.FILE + ".");
            if ((feature == Feature.VILLAGE_CLEARANCE || feature == Feature.VILLAGE_PLACEMENT)
                    && mods.containsKey("sky_village_neighbors"))
                throw new IllegalStateException("Remove Sky Village Neighbors or disable Compat's village adaptations in " + FeatureSettings.FILE);
            active.add(feature);
        }
        return new FeatureSelection(active, inactive);
    }

    public static Feature prerequisite(Feature feature) {
        return switch (feature) {
            case ORE_PLACEMENT, CARVERS, CAVE_BIOMES, ANCIENT_CITIES, SPAWN, STRUCTURE_LOCATION,
                    WATER_SPAWNS, CLIMATE, VILLAGE_PLACEMENT, PRESSURE, DELIVERY_DISTANCE,
                    DELIVERY_CHUNKS, DELIVERY_LANDING -> Feature.TERRAIN;
            default -> null;
        };
    }

    public static boolean clientOnly(Feature feature) {
        return switch (feature) {
            case DH_HANDOFF, FRAMEBUFFER, BALLOON, MATERIAL_IDS, MODEL_DATA, SINGLE_BLOCK_PASSES,
                    QUESTLOG_INTERFACE, QUESTLOG_NOTIFICATIONS -> true;
            default -> false;
        };
    }

    public static Map<String, String> dependencies(Feature feature) {
        return switch (feature) {
            case QUESTLOG_INTERFACE, QUESTLOG_NOTIFICATIONS, QUESTLOG_REWARD_VALIDATION -> Map.of("questlog", "3.5.0");
            case TERRAIN, ORE_PLACEMENT, CARVERS, CAVE_BIOMES, ANCIENT_CITIES, SPAWN,
                    STRUCTURE_LOCATION, WATER_SPAWNS, CLIMATE -> Map.of("sky_archipelago", "1.3.6");
            case VILLAGE_CLEARANCE, VILLAGE_PLACEMENT -> Map.of("sky_archipelago", "1.3.6", "skyvillages", "1.0.6", "architectury", "13.");
            case PRESSURE -> Map.of("sable", "2.0.6");
            case DELIVERY_DISTANCE, DELIVERY_CHUNKS -> Map.of("aeronautics_delivery_quests", "1.1.1");
            case DELIVERY_LANDING -> Map.of("aeronautics_delivery_quests", "1.1.1", "skyvillages", "1.0.6", "architectury", "13.");
            case ANALOG_AUDIO -> Map.of("analogaudio", "0.1.5-hotfix.1", "create", "6.0.10");
            case DH_HANDOFF -> Map.of("distanthorizons", "3.3.3");
            case DH_SHUTDOWN, DH_CACHE -> Map.of("distanthorizons", "3.3.3", "sky_archipelago", "1.3.6");
            case BLUEMAP -> Map.of("bluemap", "5.7", "sky_archipelago", "1.3.6");
            case FRAMEBUFFER -> Map.of("veil", "4.3.2", "iris", "1.8.14-beta.1+mc1.21.1");
            case BALLOON -> Map.of("aeronautics", "1.3.2", "iris", "1.8.14-beta.1+mc1.21.1");
            case MATERIAL_IDS, SINGLE_BLOCK_PASSES -> Map.of("sable", "2.0.6", "iris", "1.8.14-beta.1+mc1.21.1", "sodium", "0.8.13+mc1.21.1");
            case MODEL_DATA -> Map.of("sable", "2.0.6");
        };
    }

    public List<String> mixins(boolean client) {
        var result = new ArrayList<String>();
        for (var feature : Feature.values()) {
            if (!enabled(feature)) continue;
            result.addAll(switch (feature) {
                case TERRAIN -> List.of("GeneratorProfileMixin", "IslandFieldMixin", "ColumnResolverMixin",
                        "ColumnDataMixin", "InteriorSurfaceMixin", "ClusterPlacementMixin", "ClusterSamplerMixin",
                        "DescriptorFactoryMixin", "IslandDensityMixin", "PreparedDensityMixin", "TerrainMaterialMixin",
                        "GeologicalMaterialMixin", "OreVeinifierAccessor");
                case ORE_PLACEMENT -> List.of("OreHeightMixin");
                case CARVERS -> List.of("TerrainCarverPolicyMixin", "CarvingContextMixin", "CarverHeightMixin", "CarverLavaMixin", "WaterCarverMixin");
                case CAVE_BIOMES -> List.of("CaveBiomeMixin", "DeepDarkContextAccessor");
                case ANCIENT_CITIES -> List.of("AncientCityBiomeMixin", "AncientCityProjectionMixin");
                case SPAWN -> List.of("SpawnFallbackMixin");
                case STRUCTURE_LOCATION -> List.of("StructureLocateCheckMixin");
                case WATER_SPAWNS -> List.of("WaterAnimalMixin");
                case CLIMATE -> List.of("SurfaceClimateMixin", "SurfaceTemperatureMixin", "FreezeClimateMixin",
                        "LevelClimateMixin", "ServerClimateMixin", "PortalClimateMixin");
                case VILLAGE_CLEARANCE -> List.of("StructureSupportMixin");
                case VILLAGE_PLACEMENT -> List.of("VillageHeightMixin", "PlacementSaltAccessor", "VillageStructureStateMixin", "GeneratorStructureStateMixin");
                case PRESSURE -> List.of("DimensionPhysicsMixin");
                case DELIVERY_DISTANCE -> List.of("DeliveryDistanceMixin");
                case DELIVERY_CHUNKS -> List.of("DeliveryChunksMixin");
                case DELIVERY_LANDING -> List.of("DeliveryLandingMixin");
                case ANALOG_AUDIO -> List.of("AnalogAudioCreateMixin");
                case DH_HANDOFF -> List.of("dh.ClientWrapperHandoffMixin");
                case DH_SHUTDOWN -> List.of("dh.ChunkUnloadBudgetMixin");
                case DH_CACHE -> client ? List.of("dh.WorldLevelKeyMixin", "dh.LegacyCacheMixin") : List.of("dh.WorldLevelKeyMixin");
                case BLUEMAP -> List.of();
                case FRAMEBUFFER -> List.of("client.ShaderWrapperMixin");
                case BALLOON -> List.of("client.BalloonShadowMixin");
                case MATERIAL_IDS -> List.of("client.ConstructMaterialMixin");
                case MODEL_DATA -> List.of("client.SingleBlockModelDataMixin");
                case SINGLE_BLOCK_PASSES -> List.of("client.SingleBlockPassMixin");
                case QUESTLOG_INTERFACE -> List.of("client.QuestDetailsMixin", "client.QuestlogBookmarkMixin");
                case QUESTLOG_NOTIFICATIONS -> List.of("client.QuestNotificationMixin", "client.QuestSyncMixin");
                case QUESTLOG_REWARD_VALIDATION -> List.of("questlog.RewardClaimMixin");
            });
        }
        if (client && enabled(Feature.CLIMATE)) result.add("client.WeatherClimateMixin");
        return List.copyOf(result);
    }
}

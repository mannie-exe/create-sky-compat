package design.inherent.createskycompat.configuration;

import design.inherent.createskycompat.Feature;
import design.inherent.createskycompat.FeatureSettings;
import java.util.Arrays;
import java.util.List;

/** Navigation metadata only: activation and dependency checks belong to FeatureSelection. */
public final class SettingsCatalog {
    public enum Category { CLIENT, SERVER, WORLD }
    public enum Group {
        JOURNAL(Category.CLIENT, "questlog"), AUDIO_CLIENT(Category.CLIENT, "analog_audio"),
        DH_CLIENT(Category.CLIENT, "distant_horizons"), SABLE_CLIENT(Category.CLIENT, "sable"),
        VEIL(Category.CLIENT, "veil"), AERONAUTICS(Category.CLIENT, "aeronautics"),
        COLORWHEEL(Category.CLIENT, "colorwheel"), LIGHTS(Category.CLIENT, "lighting"),
        LINEAR_CLIENT(Category.CLIENT, "linear_bearing"),
        LINKAGE(Category.SERVER, "transmission_linkage"), LINEAR(Category.SERVER, "linear_bearing"),
        GRAVES(Category.SERVER, "yigd"), AUDIO(Category.SERVER, "analog_audio"),
        DELIVERY(Category.SERVER, "delivery"), QUESTLOG_SERVER(Category.SERVER, "questlog"),
        DH(Category.SERVER, "distant_horizons"), BLUEMAP(Category.SERVER, "bluemap"),
        TERRAIN(Category.WORLD, "sky_archipelago"), VILLAGES(Category.WORLD, "sky_villages"),
        PRESSURE(Category.WORLD, "sable");
        public final Category category;
        public final String label;
        Group(Category category, String label) { this.category = category; this.label = label; }
    }
    public enum Timing { IMMEDIATE, SERVER_START, GAME_RESTART, NEW_WORLD, RELOAD }
    public record FeatureEntry(Feature feature, Group group, String labelKey, String summaryKey, String filename, Timing timing) {}
    public static FeatureEntry entry(Feature feature) {
        return new FeatureEntry(feature, group(feature), "create_sky_compat.configuration.feature." + feature.key,
                "create_sky_compat.settings.summary." + feature.key, FeatureSettings.FILE, Timing.GAME_RESTART);
    }
    public record FileEntry(String filename, Group group, String help, List<Feature> owners) {
        public FileEntry { owners = List.copyOf(owners); }
        public String labelKey() { return "create_sky_compat.settings.file." + help + ".title"; }
        public String summaryKey() { return "create_sky_compat.settings.file." + help; }
        public Timing timing() {
            return switch (help) {
                case "journal" -> Timing.IMMEDIATE;
                case "villages" -> Timing.SERVER_START;
                case "generation", "interiors", "water", "oceans" -> Timing.NEW_WORLD;
                case "bluemap" -> Timing.RELOAD;
                default -> Timing.GAME_RESTART;
            };
        }
    }
    public static Group group(Feature feature) {
        return switch (feature) {
            case QUESTLOG_INTERFACE, QUESTLOG_NOTIFICATIONS -> Group.JOURNAL;
            case AUDIO_TIMELINE -> Group.AUDIO_CLIENT;
            case DH_HANDOFF -> Group.DH_CLIENT;
            case MODEL_DATA, MATERIAL_IDS, SINGLE_BLOCK_PASSES -> Group.SABLE_CLIENT;
            case FRAMEBUFFER -> Group.VEIL;
            case BALLOON -> Group.AERONAUTICS;
            case COLORWHEEL_SAMPLERS, COLORWHEEL_MATERIALS -> Group.COLORWHEEL;
            case LIGHTING_LIFECYCLE, SODIUM_LIGHTING -> Group.LIGHTS;
            case LINEAR_PONDER -> Group.LINEAR_CLIENT;
            case LINKAGE_INTERACTIONS, LINKAGE_JADE -> Group.LINKAGE;
            case LINEAR_BOOTSTRAP, LINEAR_CARRIAGE, LINEAR_TRANSMISSION, LINEAR_GYRO, LINEAR_JADE -> Group.LINEAR;
            case SABLE_GRAVES -> Group.GRAVES;
            case ANALOG_AUDIO -> Group.AUDIO;
            case DELIVERY_DISTANCE, DELIVERY_CHUNKS, DELIVERY_LANDING -> Group.DELIVERY;
            case QUESTLOG_REWARD_VALIDATION -> Group.QUESTLOG_SERVER;
            case DH_SHUTDOWN, DH_CACHE -> Group.DH;
            case BLUEMAP -> Group.BLUEMAP;
            case TERRAIN, ORE_PLACEMENT, CARVERS, CAVE_BIOMES, ANCIENT_CITIES, SPAWN,
                    STRUCTURE_LOCATION, WATER_SPAWNS, CLIMATE -> Group.TERRAIN;
            case VILLAGE_CLEARANCE, VILLAGE_PLACEMENT -> Group.VILLAGES;
            case PRESSURE -> Group.PRESSURE;
        };
    }
    public static List<Feature> features(Group group) {
        return Arrays.stream(Feature.values()).filter(feature -> group(feature) == group).toList();
    }
    public static List<Group> groups(Category category) {
        return Arrays.stream(Group.values()).filter(group -> group.category == category).toList();
    }
    public static final List<FileEntry> FILES = List.of(
            new FileEntry(FeatureSettings.FILE, null, "startup", List.of()),
            new FileEntry("create_sky_compat-journal-client.toml", Group.JOURNAL, "journal", List.of(Feature.QUESTLOG_INTERFACE, Feature.QUESTLOG_NOTIFICATIONS)),
            new FileEntry("create_sky_compat-common.toml", Group.VILLAGES, "villages", List.of(Feature.VILLAGE_CLEARANCE, Feature.VILLAGE_PLACEMENT)),
            new FileEntry("create_sky_compat-generation.json", Group.TERRAIN, "generation", List.of(Feature.TERRAIN)),
            new FileEntry("create_sky_compat-interiors.json", Group.TERRAIN, "interiors", List.of(Feature.TERRAIN)),
            new FileEntry("create_sky_compat-water.json", Group.TERRAIN, "water", List.of(Feature.TERRAIN)),
            new FileEntry("create_sky_compat-oceans.json", Group.TERRAIN, "oceans", List.of(Feature.TERRAIN)),
            new FileEntry("create_sky_compat-bluemap.json", Group.BLUEMAP, "bluemap", List.of(Feature.BLUEMAP)));
    private SettingsCatalog() {}
}

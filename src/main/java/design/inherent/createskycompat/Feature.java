package design.inherent.createskycompat;

/** Independently selectable adaptations. Selection happens before target classes load. */
public enum Feature {
    TERRAIN("terrain.preset"),
    ORE_PLACEMENT("terrain.ore_placement"),
    CARVERS("terrain.relative_carvers"),
    CAVE_BIOMES("terrain.cave_biomes"),
    ANCIENT_CITIES("terrain.ancient_cities"),
    SPAWN("terrain.spawn_landing"),
    STRUCTURE_LOCATION("terrain.structure_location"),
    WATER_SPAWNS("terrain.water_animal_spawns"),
    CLIMATE("terrain.climate"),
    VILLAGE_CLEARANCE("villages.clearance"),
    VILLAGE_PLACEMENT("villages.placement"),
    PRESSURE("sable.island_pressure"),
    DELIVERY_DISTANCE("delivery.horizontal_distance"),
    DELIVERY_CHUNKS("delivery.chunk_loading"),
    DELIVERY_LANDING("delivery.village_landing"),
    ANALOG_AUDIO("analog_audio.movement_registration"),
    DH_HANDOFF("distant_horizons.client_handoff"),
    DH_SHUTDOWN("distant_horizons.shutdown"),
    DH_CACHE("distant_horizons.world_cache", false),
    BLUEMAP("bluemap.viewer"),
    FRAMEBUFFER("rendering.veil_framebuffer"),
    BALLOON("rendering.balloon_shadow_pass"),
    MATERIAL_IDS("rendering.construct_material_ids"),
    MODEL_DATA("rendering.single_block_model_data"),
    SINGLE_BLOCK_PASSES("rendering.single_block_passes");

    public final String key;
    public final boolean defaultEnabled;

    Feature(String key) { this(key, true); }
    Feature(String key, boolean defaultEnabled) {
        this.key = key;
        this.defaultEnabled = defaultEnabled;
    }
}

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
    SABLE_GRAVES("yigd.construct_recovery"),
    AUDIO_TIMELINE("analog_audio.playback_timeline"),
    LINKAGE_INTERACTIONS("transmission_linkage.interactions"),
    LINKAGE_JADE("transmission_linkage.jade"),
    LINK_GUI_INTERACTIONS("redstone_link_gui.interactions"),
    LINK_GUI_RELOCATION("redstone_link_gui.relocation"),
    LINK_GUI_SABLE("redstone_link_gui.ship_access"),
    REDSTONE_LOGIC("redstone_additions.logic"),
    REDSTONE_SCRAPING("redstone_additions.scraping"),
    REDSTONE_WRENCH("redstone_additions.wrench"),
    REDSTONE_RENDERING("redstone_additions.transparency"),
    LINEAR_BOOTSTRAP("linear_bearing.bootstrap"),
    LINEAR_CARRIAGE("linear_bearing.carriage"),
    LINEAR_TRANSMISSION("linear_bearing.transmission"),
    LINEAR_GYRO("linear_bearing.gyro_sensor"),
    LINEAR_PONDER("linear_bearing.ponder"),
    LINEAR_JADE("linear_bearing.jade"),
    DH_HANDOFF("distant_horizons.client_handoff"),
    DH_SHUTDOWN("distant_horizons.shutdown"),
    DH_CACHE("distant_horizons.world_cache", false),
    BLUEMAP("bluemap.viewer"),
    FRAMEBUFFER("rendering.veil_framebuffer"),
    BALLOON("rendering.balloon_shadow_pass"),
    MATERIAL_IDS("rendering.construct_material_ids"),
    COLORWHEEL_SAMPLERS("rendering.colorwheel_samplers"),
    COLORWHEEL_MATERIALS("rendering.colorwheel_materials"),
    MODEL_DATA("rendering.single_block_model_data"),
    SINGLE_BLOCK_PASSES("rendering.single_block_passes"),
    CREATE_BACKFACES("create.glass_backfaces", false),
    FUSION_BACKFACES("fusion.glass_backfaces", false),
    CREATE_WINDOWS("create.window_culling"),
    RECHISELED_GLASS("rechiseled.glass_culling"),
    LIGHTING_LIFECYCLE("lighting.lifecycle"),
    SODIUM_LIGHTING("lighting.sodium"),
    QUESTLOG_INTERFACE("questlog.interface"),
    QUESTLOG_NOTIFICATIONS("questlog.notifications"),
    QUESTLOG_REWARD_VALIDATION("questlog.reward_validation");

    public final String key;
    public final boolean defaultEnabled;

    Feature(String key) { this(key, true); }
    Feature(String key, boolean defaultEnabled) {
        this.key = key;
        this.defaultEnabled = defaultEnabled;
    }
}

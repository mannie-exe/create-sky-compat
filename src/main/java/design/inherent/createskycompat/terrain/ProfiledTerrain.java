package design.inherent.createskycompat.terrain;

/** Assigned once, before the owning field is queried. Never a global world switch. */
public interface ProfiledTerrain {
    void createSky$configure(GenerationProfile profile);
    GenerationProfile createSky$profile();
}

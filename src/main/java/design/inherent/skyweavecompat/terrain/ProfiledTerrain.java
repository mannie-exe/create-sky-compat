package design.inherent.skyweavecompat.terrain;

/** Assigned once, before the owning field is queried. Never a global world switch. */
public interface ProfiledTerrain {
    void skyweave$configure(GenerationProfile profile);
    GenerationProfile skyweave$profile();
}

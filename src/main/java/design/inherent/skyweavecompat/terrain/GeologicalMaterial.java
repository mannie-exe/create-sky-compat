package design.inherent.skyweavecompat.terrain;

public interface GeologicalMaterial {
    void skyweave$setVeins(IslandVeins veins);
    void skyweave$setFrame(GeologicalFrame frame);
    void skyweave$setWater(WaterColumn water);
    WaterColumn skyweave$water();
}

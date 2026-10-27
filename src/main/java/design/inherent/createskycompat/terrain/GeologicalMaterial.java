package design.inherent.createskycompat.terrain;

public interface GeologicalMaterial {
    void createSky$setVeins(IslandVeins veins);
    void createSky$setFrame(GeologicalFrame frame);
    void createSky$setWater(WaterColumn water);
    WaterColumn createSky$water();
}

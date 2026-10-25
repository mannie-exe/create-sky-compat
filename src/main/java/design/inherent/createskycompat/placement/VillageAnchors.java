package design.inherent.createskycompat.placement;

import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import java.util.List;

public interface VillageAnchors {
    List<VillagePlan.Anchor> createSky$anchorsNear(int x, int z, SkyIslandSettings settings);
}

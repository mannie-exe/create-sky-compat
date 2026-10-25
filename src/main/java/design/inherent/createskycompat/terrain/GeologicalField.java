package design.inherent.createskycompat.terrain;

import org.sathrek.sky_archipelago.config.SkyIslandSettings;

public interface GeologicalField {
    /** Null in void or in an unadapted field. Companions share their own cluster's anchor frame. */
    GeologicalFrame createSky$frameAt(int x, int z, SkyIslandSettings settings);
}

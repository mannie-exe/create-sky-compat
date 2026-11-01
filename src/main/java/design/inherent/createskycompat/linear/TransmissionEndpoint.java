package design.inherent.createskycompat.linear;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

/** One reciprocal, reconstructed kinetic edge per transmission endpoint. */
public interface TransmissionEndpoint {
    KineticBlockEntity createSky$peer();
    void createSky$peer(KineticBlockEntity peer);
}

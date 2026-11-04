package design.inherent.skyweavecompat.linear;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

/** One reciprocal, reconstructed kinetic edge per transmission endpoint. */
public interface TransmissionEndpoint {
    KineticBlockEntity skyweave$peer();
    void skyweave$peer(KineticBlockEntity peer);
}

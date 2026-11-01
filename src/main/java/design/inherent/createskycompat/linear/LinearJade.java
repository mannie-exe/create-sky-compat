package design.inherent.createskycompat.linear;

import design.inherent.createskycompat.Feature;
import design.inherent.createskycompat.OptionalIntegrations;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Jade discovers this entry point itself; Linear's classes resolve only after selection. */
@WailaPlugin("linearbearing")
public final class LinearJade implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        if (OptionalIntegrations.enabled(Feature.LINEAR_JADE)) LinearJadeProvider.register(registration);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        if (OptionalIntegrations.enabled(Feature.LINEAR_JADE)) LinearJadeProvider.registerClient(registration);
    }
}

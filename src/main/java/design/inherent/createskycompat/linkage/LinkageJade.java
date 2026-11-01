package design.inherent.createskycompat.linkage;

import design.inherent.createskycompat.Feature;
import design.inherent.createskycompat.OptionalIntegrations;
import snownee.jade.api.*;

@WailaPlugin("aeronautics_utility_objects")
public final class LinkageJade implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        if (OptionalIntegrations.enabled(Feature.LINKAGE_JADE)) LinkageJadeProvider.register(registration);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        if (OptionalIntegrations.enabled(Feature.LINKAGE_JADE)) LinkageJadeProvider.registerClient(registration);
    }
}

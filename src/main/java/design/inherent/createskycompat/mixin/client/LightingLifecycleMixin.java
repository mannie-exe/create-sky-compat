package design.inherent.createskycompat.mixin.client;

import fr0nick.sabledynlights.lamb.create.ContraptionEntityEventHandler;
import fr0nick.sabledynlights.lamb.sable.SableDynLightSourceHolder;
import java.util.List;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remove upstream Create owners before its virtual-light maps are cleared. */
@Mixin(targets = "fr0nick.sabledynlights.LambModEventHandler", remap = false)
public abstract class LightingLifecycleMixin {
    @Inject(method = "onLevelUnload", at = @At("HEAD"), require = 1, allow = 1)
    private static void createSky$unload(LevelEvent.Unload event, CallbackInfo callback) {
        if (event.getLevel().isClientSide()) createSky$clearSources();
    }

    @Inject(method = "onLoggingOut", at = @At("HEAD"), require = 1, allow = 1)
    private static void createSky$disconnect(ClientPlayerNetworkEvent.LoggingOut event, CallbackInfo callback) {
        createSky$clearSources();
    }

    @Unique private static void createSky$clearSources() {
        // Upstream removes the entity from both active/pending lists and unregisters its lights.
        for (var entity : List.copyOf(CreateLightSourcesAccessor.createSky$trackedContraptions()))
            ContraptionEntityEventHandler.onContraptionEntityLeave(entity);
        SableDynLightSourceHolder.INSTANCE.clear();
        SableLightStateAccessor.createSky$lastLevel(null);
        SableLightStateAccessor.createSky$lastScan(0L);
    }
}

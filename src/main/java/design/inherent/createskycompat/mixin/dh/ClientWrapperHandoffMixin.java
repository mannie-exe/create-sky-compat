package design.inherent.createskycompat.mixin.dh;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper_neoforge;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
import design.inherent.createskycompat.dh.WrapperCache;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.lang.ref.WeakReference;
import java.util.Map;

/** Claim the old wrapper once, without holding the cache lock during DH level cleanup. */
@Mixin(value = ClientLevelWrapper_neoforge.class, remap = false)
abstract class ClientWrapperHandoffMixin {
    @WrapOperation(method = "getWrapper(Lnet/minecraft/client/multiplayer/ClientLevel;Z)Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/IClientLevelWrapper;",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0))
    private static Object createSky$publishReplacement(Map<ClientLevel, WeakReference<ClientLevelWrapper_neoforge>> cache,
            Object key, Operation<Object> original, @Local IServerKeyedClientLevel keyed) {
        if (keyed instanceof ClientLevelWrapper_neoforge replacement)
            return WrapperCache.exchange(cache, (ClientLevel)key, replacement);
        return original.call(cache, key);
    }

    @WrapOperation(method = "getWrapper(Lnet/minecraft/client/multiplayer/ClientLevel;Z)Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/IClientLevelWrapper;",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object createSky$keepPublishedReplacement(Map<?, ?> cache, Object key, Object value, Operation<Object> original) {
        // The keyed branch already published before tryUnloadFromWorld. Do not overwrite a later handoff.
        return cache.get(key);
    }

    @WrapOperation(method = "onUnload", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object createSky$removeOnlyOwnedWrapper(Map<ClientLevel, WeakReference<ClientLevelWrapper_neoforge>> cache,
            Object key, Operation<Object> original) {
        return WrapperCache.removeOwned(cache, (ClientLevel)key, (ClientLevelWrapper_neoforge)(Object)this);
    }
}

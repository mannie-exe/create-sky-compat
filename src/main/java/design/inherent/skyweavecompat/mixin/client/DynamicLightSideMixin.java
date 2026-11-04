package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import design.inherent.skyweavecompat.lighting.ClientLightScope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;

/** Do not let SDL's client lookup replace an authoritative engine query. */
@Mixin(LightEngine.class)
public abstract class DynamicLightSideMixin {
    @WrapMethod(method = "getLightValue")
    private int skyweave$clientLight(BlockPos position, Operation<Integer> original) {
        var owner = ((LightEngineOwnerAccessor)this).skyweave$lightOwner().getLevel();
        if (!(owner instanceof LevelAccessor level) || level.isClientSide()) return original.call(position);
        Boolean previous = ClientLightScope.enterServer();
        try { return original.call(position); }
        finally { ClientLightScope.restore(previous); }
    }
}

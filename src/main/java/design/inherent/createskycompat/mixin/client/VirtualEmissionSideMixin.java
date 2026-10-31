package design.inherent.createskycompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import design.inherent.createskycompat.lighting.ClientLightScope;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;

/** Preserve the full native emission method while excluding SDL's client contribution. */
@Mixin(BlockLightEngine.class)
public abstract class VirtualEmissionSideMixin {
    @WrapMethod(method = "getEmission")
    private int createSky$clientEmission(long position, BlockState state, Operation<Integer> original) {
        var owner = ((LightEngineOwnerAccessor)this).createSky$lightOwner().getLevel();
        if (!(owner instanceof LevelAccessor level) || level.isClientSide()) return original.call(position, state);
        Boolean previous = ClientLightScope.enterServer();
        try { return original.call(position, state); }
        finally { ClientLightScope.restore(previous); }
    }
}

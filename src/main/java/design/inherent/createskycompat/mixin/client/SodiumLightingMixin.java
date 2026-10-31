package design.inherent.createskycompat.mixin.client;

import fr0nick.sabledynlights.util.WorldPosResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apply SDL's coordinate-aware light lookup before Sodium caches a block's packed data. */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public abstract class SodiumLightingMixin {
    @Shadow protected BlockAndTintGetter level;
    @Shadow public static int packBL(int light) { throw new AssertionError(); }
    @Shadow public static int unpackBL(int data) { throw new AssertionError(); }

    @Inject(method = "compute(III)I", at = @At("RETURN"), cancellable = true, require = 1, allow = 1)
    private void createSky$dynamicLight(int x, int y, int z, CallbackInfoReturnable<Integer> result) {
        int data = result.getReturnValueI();
        int block = unpackBL(data);
        if (block == 15) return;
        int combined = WorldPosResolver.getCombinedWorldLight(level, new BlockPos(x, y, z), block);
        if (combined > block) result.setReturnValue((data & ~packBL(15)) | packBL(combined));
    }
}

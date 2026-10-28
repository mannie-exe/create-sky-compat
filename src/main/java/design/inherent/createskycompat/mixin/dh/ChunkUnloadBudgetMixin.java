package design.inherent.createskycompat.mixin.dh;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Queue;

/** Let the shutdown loop service queued generation cleanup between unload passes. */
@Mixin(ChunkMap.class)
abstract class ChunkUnloadBudgetMixin {
    @Shadow @Final private ServerLevel level;

    @WrapOperation(method = "processUnloads", at = @At(value = "INVOKE", target = "Ljava/util/Queue;poll()Ljava/lang/Object;"))
    private Object createSky$yieldShutdown(Queue<?> queue, Operation<Object> original,
            @Share("createSky$unloads") LocalIntRef processed) {
        if (!level.getServer().isRunning()
                && level.getChunkSource().getGenerator() instanceof SkyIslandChunkGenerator) {
            // An unfinished generation reference can requeue the same unload.
            // Shutdown supplies an always-true time budget; draining forever
            // prevents the queued DH cleanup from releasing that reference.
            if (processed.get() >= 1024) return null;
            processed.set(processed.get() + 1);
        }
        return original.call(queue);
    }
}

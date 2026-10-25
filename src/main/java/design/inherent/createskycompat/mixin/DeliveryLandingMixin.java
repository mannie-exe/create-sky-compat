package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.placement.DeliveryChunks;
import design.inherent.createskycompat.placement.VillageDeliveryLanding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.util.concurrent.CompletableFuture;

@Pseudo
@Mixin(targets = "com.ladderstar.adq.QuestGenerator", remap = false)
public abstract class DeliveryLandingMixin {
    @WrapOperation(method = "lambda$generateNewQuestAsync$9", require = 2, allow = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private static double createSky$horizontalRouteDistance(BlockPos point, Vec3i other, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        if (!DeliveryChunks.applies(level)) return original.call(point, other);
        // Locator Y can be zero before realization and ~500 afterward, even at the same village.
        double dx = (double)point.getX() - other.getX(), dz = (double)point.getZ() - other.getZ();
        return dx * dx + dz * dz;
    }

    @WrapOperation(method = "lambda$generateNewQuestAsync$9", require = 2, allow = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;getChunkFuture(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<ChunkResult<ChunkAccess>> createSky$loadDeliveryChunks(ServerChunkCache cache,
            int x, int z, ChunkStatus status, boolean create, Operation<CompletableFuture<ChunkResult<ChunkAccess>>> original) {
        return DeliveryChunks.applies(cache, status, create) ? DeliveryChunks.request(cache, x, z)
                : original.call(cache, x, z, status, create);
    }

    @Inject(method = "registerGeneratedQuest", at = @At("HEAD"), require = 1)
    private static void createSky$releaseSuccessfulRoute(CallbackInfo callback, @Local(argsOnly = true) ServerLevel level) {
        DeliveryChunks.finish(level);
    }

    @Inject(method = "failGeneration", at = @At("HEAD"), require = 1)
    private static void createSky$releaseFailedRoute(CallbackInfo callback, @Local(argsOnly = true) ServerLevel level) {
        DeliveryChunks.finish(level);
    }

    @WrapOperation(method = "lambda$generateNewQuestAsync$4", require = 2, allow = 2,
            at = @At(value = "INVOKE", target = "Lcom/ladderstar/adq/QuestGenerator;resolveDrySurface(Lnet/minecraft/server/level/ServerLevel;II)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos createSky$deliveryLanding(ServerLevel level, int x, int z, Operation<BlockPos> original) {
        var result = original.call(level, x, z);
        return result != null ? result : VillageDeliveryLanding.find(level, x, z, (world, px, pz) -> original.call(world, px, pz));
    }
}

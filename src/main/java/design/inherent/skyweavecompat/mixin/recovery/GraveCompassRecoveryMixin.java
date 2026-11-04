package design.inherent.skyweavecompat.mixin.recovery;

import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import design.inherent.skyweavecompat.recovery.ShipGraves;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.b1n_ry.yigd.config.ExtraFeaturesConfig;
import com.b1n_ry.yigd.config.YigdConfig;

/** Project linked compasses without changing the grave's physical storage address. */
@Mixin(value = GraveCompassHelper.class, remap = false)
public abstract class GraveCompassRecoveryMixin {
    @Inject(method = "findClosest(Ljava/util/UUID;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;",
            at = @At("RETURN"), cancellable = true)
    private static void skyweave$nearestInWorld(UUID owner, ResourceKey<Level> dimension, BlockPos origin,
            CallbackInfoReturnable<BlockPos> cir) {
        if (YigdConfig.getConfig().extraFeatures.graveCompass.pointToClosest
                != ExtraFeaturesConfig.GraveCompassConfig.CompassGraveTarget.DISABLED)
            cir.setReturnValue(ShipGraves.closestGrave(owner, dimension, origin, cir.getReturnValue()));
    }
    @WrapMethod(method = "giveCompass")
    private static void skyweave$projectCompass(ServerPlayer player, UUID id, BlockPos position,
            ResourceKey<Level> dimension, Operation<Void> original) {
        var grave = DeathInfoManager.INSTANCE.getGrave(id).orElse(null);
        original.call(player, id, grave != null && ShipGraves.anchor(grave) != null
                ? ShipGraves.worldPosition(grave) : position, dimension);
    }
}

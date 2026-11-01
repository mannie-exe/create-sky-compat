package design.inherent.createskycompat.mixin.recovery;

import com.b1n_ry.yigd.components.GraveComponent;
import design.inherent.createskycompat.recovery.GraveRecoveryState;
import design.inherent.createskycompat.recovery.RecoveryAnchor;
import design.inherent.createskycompat.recovery.ShipGraves;
import com.b1n_ry.yigd.networking.LightGraveData;
import com.b1n_ry.yigd.data.DirectionalPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GraveComponent.class, remap = false)
public abstract class GraveRecoveryMixin implements GraveRecoveryState {
    @Unique private RecoveryAnchor createSky$anchor;
    @Override public RecoveryAnchor createSky$anchor() { return createSky$anchor; }
    @Override public void createSky$anchor(RecoveryAnchor anchor) { createSky$anchor = anchor; }

    @Inject(method = "findGravePos", at = @At("RETURN"), cancellable = true)
    private void createSky$respectDimensionFloor(net.minecraft.core.Direction direction, CallbackInfoReturnable<DirectionalPos> cir) {
        var grave = (GraveComponent)(Object)this;
        var world = grave.getWorld();
        if (world == null) return;
        var result = cir.getReturnValue();
        int y = Math.clamp(result.pos().getY(), world.getMinBuildHeight() + 1, world.getMaxBuildHeight() - 1);
        if (y != result.pos().getY()) {
            var position = new BlockPos(result.pos().getX(), y, result.pos().getZ());
            grave.setPos(position);
            cir.setReturnValue(new DirectionalPos(position, result.dir()));
        }
    }

    @Inject(method = "onDestroyed", at = @At("HEAD"), cancellable = true)
    private void createSky$preserveRecovery(CallbackInfo ci) {
        if (ShipGraves.preserveMissingGrave((GraveComponent)(Object)this)) ci.cancel();
    }

    @Inject(method = "claim", at = @At("HEAD"), cancellable = true)
    private void createSky$validateClaim(ServerPlayer player, ServerLevel level, BlockState previous,
            BlockPos pos, ItemStack tool, CallbackInfoReturnable<InteractionResult> cir) {
        var grave = (GraveComponent)(Object)this;
        if (createSky$anchor != null && (!ShipGraves.currentPhysicalGrave(grave, level, pos)
                || player.level() != level
                || player.position().distanceToSqr(ShipGraves.worldCenter(grave))
                        > Math.pow(player.blockInteractionRange() + 1, 2))) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "toLightData", at = @At("RETURN"), cancellable = true)
    private void createSky$projectDisplay(CallbackInfoReturnable<LightGraveData> cir) {
        if (createSky$anchor == null) return;
        var data = cir.getReturnValue();
        cir.setReturnValue(new LightGraveData(data.itemCount(), ShipGraves.worldPosition((GraveComponent)(Object)this),
                data.xpPoints(), data.registryKey(), data.deathMessage(), data.id(), data.status()));
    }

    @Inject(method = "toNbt", at = @At("RETURN"))
    private void createSky$save(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        if (createSky$anchor != null) cir.getReturnValue().put("create_sky_compat:recovery", createSky$anchor.save());
    }

    @Inject(method = "fromNbt", at = @At("RETURN"))
    private static void createSky$read(CompoundTag tag, HolderLookup.Provider registries,
            MinecraftServer server, CallbackInfoReturnable<GraveComponent> cir) {
        if (cir.getReturnValue() != null && tag.contains("create_sky_compat:recovery"))
            ((GraveRecoveryState)(Object)cir.getReturnValue()).createSky$anchor(
                    RecoveryAnchor.read(tag.getCompound("create_sky_compat:recovery")));
    }
}

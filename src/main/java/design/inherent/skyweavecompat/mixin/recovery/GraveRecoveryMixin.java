package design.inherent.skyweavecompat.mixin.recovery;

import com.b1n_ry.yigd.components.GraveComponent;
import design.inherent.skyweavecompat.recovery.GraveRecoveryState;
import design.inherent.skyweavecompat.recovery.RecoveryAnchor;
import design.inherent.skyweavecompat.recovery.ShipGraves;
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
    @Unique private RecoveryAnchor skyweave$anchor;
    @Override public RecoveryAnchor skyweave$anchor() { return skyweave$anchor; }
    @Override public void skyweave$anchor(RecoveryAnchor anchor) { skyweave$anchor = anchor; }

    @Inject(method = "findGravePos", at = @At("RETURN"), cancellable = true)
    private void skyweave$respectDimensionFloor(net.minecraft.core.Direction direction, CallbackInfoReturnable<DirectionalPos> cir) {
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
    private void skyweave$preserveRecovery(CallbackInfo ci) {
        if (ShipGraves.preserveMissingGrave((GraveComponent)(Object)this)) ci.cancel();
    }

    @Inject(method = "claim", at = @At("HEAD"), cancellable = true)
    private void skyweave$validateClaim(ServerPlayer player, ServerLevel level, BlockState previous,
            BlockPos pos, ItemStack tool, CallbackInfoReturnable<InteractionResult> cir) {
        var grave = (GraveComponent)(Object)this;
        if (skyweave$anchor != null && (!ShipGraves.currentPhysicalGrave(grave, level, pos)
                || player.level() != level
                || player.position().distanceToSqr(ShipGraves.worldCenter(grave))
                        > Math.pow(player.blockInteractionRange() + 1, 2))) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "toLightData", at = @At("RETURN"), cancellable = true)
    private void skyweave$projectDisplay(CallbackInfoReturnable<LightGraveData> cir) {
        if (skyweave$anchor == null) return;
        var data = cir.getReturnValue();
        cir.setReturnValue(new LightGraveData(data.itemCount(), ShipGraves.worldPosition((GraveComponent)(Object)this),
                data.xpPoints(), data.registryKey(), data.deathMessage(), data.id(), data.status()));
    }

    @Inject(method = "toNbt", at = @At("RETURN"))
    private void skyweave$save(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        if (skyweave$anchor != null) cir.getReturnValue().put("skyweave_compat:recovery", skyweave$anchor.save());
    }

    @Inject(method = "fromNbt", at = @At("RETURN"))
    private static void skyweave$read(CompoundTag tag, HolderLookup.Provider registries,
            MinecraftServer server, CallbackInfoReturnable<GraveComponent> cir) {
        if (cir.getReturnValue() != null && tag.contains("skyweave_compat:recovery"))
            ((GraveRecoveryState)(Object)cir.getReturnValue()).skyweave$anchor(
                    RecoveryAnchor.read(tag.getCompound("skyweave_compat:recovery")));
    }
}

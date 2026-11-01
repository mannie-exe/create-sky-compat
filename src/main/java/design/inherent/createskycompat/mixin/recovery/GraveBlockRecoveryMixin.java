package design.inherent.createskycompat.mixin.recovery;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import design.inherent.createskycompat.recovery.ShipGraves;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Mixin(value = GraveBlockEntity.class, remap = false)
public abstract class GraveBlockRecoveryMixin {
    @Shadow private GraveComponent component;

    @WrapOperation(method = "updatePosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent createSky$projectRelocation(String key, Object[] arguments, Operation<MutableComponent> original) {
        if (component != null && ShipGraves.anchor(component) != null && key.equals("text.yigd.message.grave_relocated")) {
            var position = ShipGraves.worldPosition(component);
            return original.call(key, new Object[]{position.getX(), position.getY(), position.getZ(), component.getWorldRegistryKey().location().toString()});
        }
        return original.call(key, arguments);
    }

    @Inject(method = "updatePosition", at = @At("HEAD"), cancellable = true)
    private void createSky$validateRelocation(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (component != null && !ShipGraves.acceptRelocation(component, level, pos)) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }
}

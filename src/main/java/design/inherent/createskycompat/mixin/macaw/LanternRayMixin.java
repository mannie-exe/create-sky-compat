package design.inherent.createskycompat.mixin.macaw;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import design.inherent.createskycompat.macaw.LanternSelection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockGetter.class)
public interface LanternRayMixin {
    @ModifyReturnValue(method = "clip", at = @At("RETURN"), require = 1)
    private BlockHitResult createSky$selectOverhangingLantern(BlockHitResult hit, ClipContext context) {
        return LanternSelection.extend((BlockGetter) this, context, hit);
    }
}

package design.inherent.createskycompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.sublevel.render.vanilla.SingleBlockSubLevelWrapper;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Render-layer selection must read the same model data as Sable's tessellation call. */
@Mixin(targets = "dev.ryanhcode.sable.neoforge.platform.SableSubLevelRenderPlatformImpl", remap = false)
public abstract class SingleBlockModelDataMixin {
    @WrapOperation(method = "getRenderLayers", at = @At(value = "INVOKE",
            target = "Ldev/ryanhcode/sable/sublevel/render/vanilla/SingleBlockSubLevelWrapper;getModelData(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/client/model/data/ModelData;"),
            require = 1, allow = 1)
    private ModelData createSky$matchingModelData(SingleBlockSubLevelWrapper view, BlockPos position, Operation<ModelData> original) {
        return view.getLevel().getModelData(position);
    }
}

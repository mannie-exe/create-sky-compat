package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import design.inherent.skyweavecompat.client.GlassBackfaces;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve Fusion's processor and modifier ownership; extend only final quads. */
@Mixin(targets = "com.supermartijn642.fusion.model.types.base.BaseBakedModel", remap = false)
abstract class FusionGlassBackfaceMixin {
    @ModifyReturnValue(method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;",
            at = @At("RETURN"), require = 1, allow = 3)
    private List<BakedQuad> skyweave$backfaces(List<BakedQuad> quads, BlockState state) {
        return GlassBackfaces.fusionTarget(state) ? GlassBackfaces.append(quads) : quads;
    }
}

package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock;
import com.simibubi.create.content.decoration.palettes.GlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.WindowBlock;
import design.inherent.skyweavecompat.client.GlassBackfaces;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reverse the final UV selection, never feed reversed normals into CT lookup. */
@Mixin(targets = "com.simibubi.create.foundation.block.connected.CTModel", remap = false)
abstract class CreateGlassBackfaceMixin {
    @ModifyReturnValue(method = "getQuads", at = @At("RETURN"), require = 1, allow = 2)
    private List<BakedQuad> skyweave$backfaces(List<BakedQuad> quads, BlockState state) {
        if (state == null) return quads;
        var block = state.getBlock();
        return block instanceof ConnectedGlassBlock || block instanceof WindowBlock || block instanceof GlassPaneBlock
                ? GlassBackfaces.append(quads) : quads;
    }
}

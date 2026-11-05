package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock;
import com.simibubi.create.content.decoration.palettes.GlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.WindowBlock;
import design.inherent.skyweavecompat.client.GlassBackfaces;
import design.inherent.skyweavecompat.client.GlassSurfaceContacts;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reverse the final UV selection, never feed reversed normals into CT lookup. */
@Mixin(targets = "com.simibubi.create.foundation.block.connected.CTModel", remap = false)
abstract class CreateGlassBackfaceMixin {
    @ModifyReturnValue(method = "getQuads", at = @At("RETURN"), require = 1, allow = 2)
    private List<BakedQuad> skyweave$backfaces(List<BakedQuad> quads, BlockState state, Direction side,
            RandomSource random, ModelData data, RenderType layer) {
        if (state == null) return quads;
        var block = state.getBlock();
        return block instanceof ConnectedGlassBlock || block instanceof WindowBlock || block instanceof GlassPaneBlock
                ? GlassBackfaces.append(quads, data.get(GlassSurfaceContacts.PROPERTY)) : quads;
    }

    @ModifyReturnValue(method = "gatherModelData", at = @At("RETURN"), require = 1)
    private ModelData.Builder skyweave$contacts(ModelData.Builder result, ModelData.Builder builder,
            BlockAndTintGetter view, BlockPos pos, BlockState state, ModelData original) {
        var block = state.getBlock();
        if (block instanceof ConnectedGlassBlock || block instanceof WindowBlock || block instanceof GlassPaneBlock) {
            var contacts = GlassSurfaceContacts.capture(view, pos);
            if (contacts != null) result.with(GlassSurfaceContacts.PROPERTY, contacts);
        }
        return result;
    }
}

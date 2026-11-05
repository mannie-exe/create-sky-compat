package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

/** Preserve Fusion's processor and modifier ownership; extend only final quads. */
@Mixin(targets = "com.supermartijn642.fusion.model.types.base.BaseBakedModel", remap = false)
abstract class FusionGlassBackfaceMixin {
    @ModifyReturnValue(method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;",
            at = @At("RETURN"), require = 1, allow = 3)
    private List<BakedQuad> skyweave$backfaces(List<BakedQuad> quads, BlockState state, Direction side,
            RandomSource random, ModelData data, RenderType layer) {
        return GlassBackfaces.fusionTarget(state) ? GlassBackfaces.append(quads, data.get(GlassSurfaceContacts.PROPERTY)) : quads;
    }

    @ModifyReturnValue(method = "getModelData", at = @At("RETURN"), require = 1)
    private ModelData skyweave$contacts(ModelData result, BlockAndTintGetter view, BlockPos pos,
            BlockState state, ModelData original) {
        if (!GlassBackfaces.fusionTarget(state)) return result;
        var contacts = GlassSurfaceContacts.capture(view, pos);
        return contacts == null ? result : result.derive().with(GlassSurfaceContacts.PROPERTY, contacts).build();
    }
}

package design.inherent.skyweavecompat.client;

import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The two Create glass models that do not pass through Create's CTModel. */
public final class PlainGlassModels extends BakedModelWrapper<BakedModel> {
    public PlainGlassModels(BakedModel original) { super(original); }

    public static void wrap(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) -> id.id().getNamespace().equals("create")
                && (id.id().getPath().equals("tiled_glass") || id.id().getPath().equals("tiled_glass_pane"))
                && !id.getVariant().equals("inventory") && !(model instanceof PlainGlassModels)
                ? new PlainGlassModels(model) : model);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter view, BlockPos pos, BlockState state, ModelData incoming) {
        var data = originalModel.getModelData(view, pos, state, incoming);
        var contacts = GlassSurfaceContacts.capture(view, pos);
        return contacts == null ? data : data.derive().with(GlassSurfaceContacts.PROPERTY, contacts).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType layer) {
        var quads = originalModel.getQuads(state, side, random, data, layer);
        return state == null ? quads : GlassBackfaces.append(quads, data.get(GlassSurfaceContacts.PROPERTY));
    }
}

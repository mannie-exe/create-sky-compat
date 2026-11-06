package design.inherent.skyweavecompat.client;

import design.inherent.skyweavecompat.Feature;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Final-quad wrapper for explicitly admitted static glass models, independent of their mod. */
public final class PlainGlassModels extends BakedModelWrapper<BakedModel> {
    public PlainGlassModels(BakedModel original) { super(original); }

    public static final Set<Feature> FEATURES = Set.of(Feature.CREATE_BACKFACES, Feature.FUSION_BACKFACES,
            Feature.MACAW_GLASS, Feature.CONFECTIONERY_BACKFACES, Feature.BUBBLE_GUM_BACKFACES,
            Feature.ORNATE_GRATE_BACKFACES, Feature.INDUSTRIAL_GRATING_BACKFACES);

    public static boolean target(ResourceLocation id, Set<Feature> active) {
        String path = id.getPath();
        return switch (id.getNamespace()) {
            case "create" -> active.contains(Feature.CREATE_BACKFACES)
                    && (path.equals("tiled_glass") || path.equals("tiled_glass_pane"));
            case "minecraft" -> active.contains(Feature.FUSION_BACKFACES) && path.equals("tinted_glass");
            case "mcwwindows" -> active.contains(Feature.MACAW_GLASS)
                    && (path.endsWith("_mosaic_glass") || path.endsWith("_mosaic_glass_pane"));
            case "create_confectionery" -> active.contains(Feature.CONFECTIONERY_BACKFACES)
                    && (path.equals("caramel_bricks") || path.equals("caramel_bricks_slab") || path.equals("caramel_bricks_stairs"));
            case "linearbearing" -> active.contains(Feature.BUBBLE_GUM_BACKFACES) && path.equals("bubble_gum_block");
            case "dndecor" -> active.contains(Feature.ORNATE_GRATE_BACKFACES) && path.equals("ornate_grate");
            case "bits_n_bobs" -> active.contains(Feature.INDUSTRIAL_GRATING_BACKFACES) && path.equals("industrial_grating");
            default -> false;
        };
    }

    public static void wrap(ModelEvent.ModifyBakingResult event, Predicate<ResourceLocation> target) {
        event.getModels().replaceAll((id, model) -> target.test(id.id())
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

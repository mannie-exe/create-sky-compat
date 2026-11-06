package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.client.PlainGlassModels;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Set;
import design.inherent.skyweavecompat.Feature;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlainGlassModelsTest {
    @Test void wrapsOnlyTiledBlockStatesIncludingMultipartStatesAndRetainsOtherOwners() {
        var source = (BakedModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{BakedModel.class},
                (proxy, method, args) -> { throw new AssertionError("Bake registration must not query models"); });
        var models = new HashMap<ModelResourceLocation, BakedModel>();
        var block = new ModelResourceLocation(ResourceLocation.parse("create:tiled_glass"), "");
        var pane = new ModelResourceLocation(ResourceLocation.parse("create:tiled_glass_pane"), "north=true,east=false");
        var item = new ModelResourceLocation(ResourceLocation.parse("create:tiled_glass"), "inventory");
        var other = new ModelResourceLocation(ResourceLocation.parse("other:tiled_glass"), "");
        var ct = new ModelResourceLocation(ResourceLocation.parse("create:framed_glass"), "");
        for (var id : java.util.List.of(block, pane, item, other, ct)) models.put(id, source);
        var event = new ModelEvent.ModifyBakingResult(models, null, null);
        PlainGlassModels.wrap(event, id -> PlainGlassModels.target(id, Set.of(Feature.CREATE_BACKFACES)));
        assertInstanceOf(PlainGlassModels.class, models.get(block));
        assertInstanceOf(PlainGlassModels.class, models.get(pane));
        for (var id : java.util.List.of(item, other, ct)) assertSame(source, models.get(id));
        var wrapped = models.get(pane);
        PlainGlassModels.wrap(event, id -> PlainGlassModels.target(id, Set.of(Feature.CREATE_BACKFACES)));
        assertSame(wrapped, models.get(pane));
    }
    @Test void admitsMosaicStatesAndTintedGlassWithoutChangingOtherWindowSemantics() {
        var source = (BakedModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{BakedModel.class},
                (proxy, method, args) -> { throw new AssertionError("Registration must not query models"); });
        var models = new HashMap<ModelResourceLocation, BakedModel>();
        for (String part : java.util.List.of("base", "top", "middle", "bottom"))
            models.put(new ModelResourceLocation(ResourceLocation.parse("mcwwindows:red_mosaic_glass"), "part=" + part), source);
        var pane = new ModelResourceLocation(ResourceLocation.parse("mcwwindows:red_mosaic_glass_pane"), "north=true");
        var tinted = new ModelResourceLocation(ResourceLocation.parse("minecraft:tinted_glass"), "");
        models.put(pane, source); models.put(tinted, source);
        var controls = java.util.List.of("mcwwindows:one_way_glass", "mcwwindows:one_way_glass_pane",
                "mcwwindows:oak_window", "other:red_mosaic_glass");
        for (String id : controls) models.put(new ModelResourceLocation(ResourceLocation.parse(id), ""), source);
        var item = new ModelResourceLocation(ResourceLocation.parse("mcwwindows:red_mosaic_glass"), "inventory");
        models.put(item, source);
        var event = new ModelEvent.ModifyBakingResult(models, null, null);
        PlainGlassModels.wrap(event, id -> PlainGlassModels.target(id, Set.of(Feature.FUSION_BACKFACES)));
        assertInstanceOf(PlainGlassModels.class, models.get(tinted));
        assertSame(source, models.get(pane), "Macaw selection stays independent of Fusion");
        PlainGlassModels.wrap(event, id -> PlainGlassModels.target(id, Set.of(Feature.MACAW_GLASS)));
        assertInstanceOf(PlainGlassModels.class, models.get(pane));
        for (String part : java.util.List.of("base", "top", "middle", "bottom"))
            assertInstanceOf(PlainGlassModels.class, models.get(new ModelResourceLocation(
                    ResourceLocation.parse("mcwwindows:red_mosaic_glass"), "part=" + part)));
        for (String id : controls) assertSame(source, models.get(new ModelResourceLocation(ResourceLocation.parse(id), "")));
        assertSame(source, models.get(item));
    }

    @Test void approvedFamiliesAreIndependentAndExcludeScaffoldingAndUnapprovedVariants() {
        var targets = java.util.Map.of(
                Feature.CONFECTIONERY_BACKFACES, java.util.List.of("create_confectionery:caramel_bricks",
                        "create_confectionery:caramel_bricks_slab", "create_confectionery:caramel_bricks_stairs"),
                Feature.BUBBLE_GUM_BACKFACES, java.util.List.of("linearbearing:bubble_gum_block"),
                Feature.ORNATE_GRATE_BACKFACES, java.util.List.of("dndecor:ornate_grate"),
                Feature.INDUSTRIAL_GRATING_BACKFACES, java.util.List.of("bits_n_bobs:industrial_grating"));
        for (var entry : targets.entrySet()) for (String id : entry.getValue()) {
            assertTrue(PlainGlassModels.target(ResourceLocation.parse(id), Set.of(entry.getKey())));
            var disabled = new java.util.HashSet<>(PlainGlassModels.FEATURES);
            disabled.remove(entry.getKey());
            assertFalse(PlainGlassModels.target(ResourceLocation.parse(id), disabled));
        }
        for (String id : java.util.List.of("minecraft:scaffolding", "create:andesite_scaffolding",
                "create:brass_scaffolding", "create:copper_scaffolding", "create_confectionery:caramel",
                "bits_n_bobs:industrial_grating_panel", "dndecor:ornate_iron_glass"))
            assertFalse(PlainGlassModels.target(ResourceLocation.parse(id), PlainGlassModels.FEATURES));
    }

}

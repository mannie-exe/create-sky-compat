package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.client.PlainGlassModels;
import java.lang.reflect.Proxy;
import java.util.HashMap;
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
        PlainGlassModels.wrap(event);
        assertInstanceOf(PlainGlassModels.class, models.get(block));
        assertInstanceOf(PlainGlassModels.class, models.get(pane));
        for (var id : java.util.List.of(item, other, ct)) assertSame(source, models.get(id));
        var wrapped = models.get(pane);
        PlainGlassModels.wrap(event);
        assertSame(wrapped, models.get(pane));
    }
}

package design.inherent.skyweavecompat.mixin.client;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "fr0nick.sabledynlights.lamb.create.ContraptionEntityEventHandler", remap = false)
public interface CreateLightSourcesAccessor {
    @Accessor("contraptionEntities")
    static List<AbstractContraptionEntity> skyweave$trackedContraptions() { throw new AssertionError(); }
}

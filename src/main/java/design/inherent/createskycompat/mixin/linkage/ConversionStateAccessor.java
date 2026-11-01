package design.inherent.createskycompat.mixin.linkage;

import com.enxv.aerouniversaljoint.content.DampingStressBearingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read upstream's sampled suppression result; Jade does not run another structure scan. */
@Mixin(value = DampingStressBearingBlockEntity.class, remap = false)
public interface ConversionStateAccessor {
    @Accessor("stressOutputSuppressed") boolean createSky$suppressed();
    @Accessor("suppressedByBlockId") String createSky$suppressedBy();
}

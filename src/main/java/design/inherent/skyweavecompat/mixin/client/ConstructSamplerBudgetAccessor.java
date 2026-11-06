package design.inherent.skyweavecompat.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.irisshaders.iris.gl.program.ProgramSamplers$Builder", remap = false)
public interface ConstructSamplerBudgetAccessor {
    @Accessor("remainingUnits") int skyweave$remainingUnits();
}

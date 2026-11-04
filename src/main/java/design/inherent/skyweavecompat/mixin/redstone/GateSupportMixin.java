package design.inherent.skyweavecompat.mixin.redstone;

import dev.siliconcarbidecube.create_redstone_additions.blocks.AbstractResistor;
import dev.siliconcarbidecube.create_redstone_additions.blocks.Diode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {Diode.class, AbstractResistor.class}, remap = false)
public abstract class GateSupportMixin extends DiodeBlock {
    protected GateSupportMixin(BlockBehaviour.Properties properties) { super(properties); }
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void skyweave$support(BlockState state, Level level, BlockPos pos, Block from, BlockPos fromPos, boolean moving, CallbackInfo ci) {
        if (!state.canSurvive(level, pos)) { super.neighborChanged(state, level, pos, from, fromPos, moving); ci.cancel(); }
    }
}

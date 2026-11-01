package design.inherent.createskycompat.mixin.redstone;

import dev.siliconcarbidecube.create_redstone_additions.blocks.Disjunctor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Disjunctor.class, remap = false)
public abstract class DisjunctorMixin extends DiodeBlock {
    protected DisjunctorMixin(BlockBehaviour.Properties properties) { super(properties); }
    @Inject(method = "<init>", at = @At("RETURN"), require = 1, allow = 1)
    private void createSky$default(BlockBehaviour.Properties properties, CallbackInfo ci) {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }
}

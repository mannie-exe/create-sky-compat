package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.GeologicalFrame;
import design.inherent.skyweavecompat.terrain.GeologicalMaterial;
import design.inherent.skyweavecompat.terrain.IslandSurfaceScope;
import com.llamalad7.mixinextras.sugar.Local;
import org.sathrek.sky_archipelago.worldgen.generator.terrain.SkyIslandColumnMaterialPlan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import design.inherent.skyweavecompat.terrain.IslandVeins;

@Mixin(value = SkyIslandColumnMaterialPlan.class, remap = false)
public abstract class GeologicalMaterialMixin implements GeologicalMaterial {
    @Unique private GeologicalFrame skyweave$frame;
    @Unique private IslandVeins skyweave$veins;
    @Override public void skyweave$setFrame(GeologicalFrame frame) { skyweave$frame = frame; }
    @Override public void skyweave$setVeins(IslandVeins veins) { skyweave$veins = veins; }

    @Inject(method = "plannedStateAt(ILnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), cancellable = true)
    private void veins(int y, BlockState stone, BlockState deepslate, BlockState oceanFluid, BlockState riverFluid, BlockState bedrock,
            CallbackInfoReturnable<BlockState> result) {
        var state = result.getReturnValue();
        if (state == null || !state.is(Blocks.STONE) && !state.is(Blocks.DEEPSLATE)) return;
        var plan = (SkyIslandColumnMaterialPlan)(Object)this;
        if (skyweave$frame == null || skyweave$veins == null) return;
        var vein = skyweave$veins.sample(plan.worldX(), y, plan.worldZ(), skyweave$frame);
        if (vein != null) result.setReturnValue(vein);
    }

    @ModifyVariable(method = "usesDeepslateAtY(I)Z", at = @At("HEAD"), argsOnly = true)
    private int geologicalY(int y) {
        return skyweave$frame == null ? y : skyweave$frame.toReference(y);
    }

    @ModifyVariable(method = "resolveSurfaceStone", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static int surfaceGeologicalY(int y, @Local(argsOnly = true, ordinal = 0) int x,
            @Local(argsOnly = true, ordinal = 2) int z) {
        return IslandSurfaceScope.referenceY(x, y, z);
    }
}

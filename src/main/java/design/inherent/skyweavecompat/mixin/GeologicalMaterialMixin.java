package design.inherent.skyweavecompat.mixin;

import design.inherent.skyweavecompat.terrain.GeologicalFrame;
import design.inherent.skyweavecompat.terrain.GeologicalMaterial;
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
import design.inherent.skyweavecompat.terrain.WaterColumn;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SkyIslandColumnMaterialPlan.class, remap = false)
public abstract class GeologicalMaterialMixin implements GeologicalMaterial {
    @Unique private GeologicalFrame skyweave$frame;
    @Unique private IslandVeins skyweave$veins;
    @Unique private WaterColumn skyweave$water = WaterColumn.EMPTY;
    @Override public void skyweave$setWater(WaterColumn water) { skyweave$water = water; }
    @Override public WaterColumn skyweave$water() { return skyweave$water; }
    @Override public void skyweave$setFrame(GeologicalFrame frame) { skyweave$frame = frame; }
    @Override public void skyweave$setVeins(IslandVeins veins) { skyweave$veins = veins; }

    @Inject(method = "forEachMaterialRange", at = @At("RETURN"))
    private void waterRanges(SkyIslandColumnMaterialPlan.MaterialRangeConsumer consumer, CallbackInfo callback) {
        for (var range : skyweave$water.sources()) consumer.accept(range.bottomY(), range.topY());
    }
    @Inject(method = "firstFreeY", at = @At("RETURN"), cancellable = true)
    private void waterHeight(CallbackInfoReturnable<Integer> result) {
        result.setReturnValue(Math.max(result.getReturnValue(), skyweave$water.top(Integer.MIN_VALUE) + 1));
    }
    @Inject(method = "materialSlotAt", at = @At("HEAD"), cancellable = true)
    private void waterSlot(int y, CallbackInfoReturnable<SkyIslandColumnMaterialPlan.MaterialSlot> result) {
        if (skyweave$water.contains(y)) result.setReturnValue(SkyIslandColumnMaterialPlan.MaterialSlot.OCEAN);
    }
    @Inject(method = "plannedStateAt", at = @At("HEAD"), cancellable = true)
    private void waterState(int y, BlockState stone, BlockState deepslate, BlockState water, BlockState bedrock,
            CallbackInfoReturnable<BlockState> result) {
        if (skyweave$water.contains(y)) result.setReturnValue(Blocks.WATER.defaultBlockState());
    }

    @Inject(method = "plannedStateAt", at = @At("RETURN"), cancellable = true)
    private void veins(int y, BlockState stone, BlockState deepslate, BlockState water, BlockState bedrock,
            CallbackInfoReturnable<BlockState> result) {
        var state = result.getReturnValue();
        if (state == null || !state.is(Blocks.STONE) && !state.is(Blocks.DEEPSLATE)) return;
        var plan = (SkyIslandColumnMaterialPlan)(Object)this;
        for (var region : skyweave$water.regions()) {
            if (region.basin(plan.worldX(), plan.worldZ()) && y == region.bed(plan.worldX(), plan.worldZ())
                    && !region.shaft(plan.worldX(), plan.worldZ())) {
                long patch = Math.floorDiv(plan.worldX(), 4) * 341873128712L
                        ^ Math.floorDiv(plan.worldZ(), 4) * 132897987541L ^ region.owner();
                result.setReturnValue((region.ocean() ? Math.floorMod(patch, 5) == 0 ? Blocks.GRAVEL : Blocks.SAND
                        : Math.floorMod(patch, 5) == 0 ? Blocks.CLAY : Blocks.GRAVEL).defaultBlockState());
                return;
            }
        }
        if (skyweave$frame == null || skyweave$veins == null) return;
        var vein = skyweave$veins.sample(plan.worldX(), y, plan.worldZ(), skyweave$frame);
        if (vein != null) result.setReturnValue(vein);
    }

    @ModifyVariable(method = "usesDeepslateAtY", at = @At("HEAD"), argsOnly = true)
    private int geologicalY(int y) {
        return skyweave$frame == null ? y : skyweave$frame.toReference(y);
    }
}

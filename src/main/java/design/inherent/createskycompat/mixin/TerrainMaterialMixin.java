package design.inherent.createskycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.createskycompat.terrain.GeologicalField;
import design.inherent.createskycompat.terrain.GeologicalMaterial;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.sathrek.sky_archipelago.worldgen.generator.terrain.SkyIslandColumnMaterialPlan;
import org.sathrek.sky_archipelago.worldgen.generator.terrain.SkyIslandTerrainPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import design.inherent.createskycompat.terrain.InteriorField;
import design.inherent.createskycompat.terrain.WaterPostprocessing;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

@Mixin(value = SkyIslandTerrainPass.class, remap = false)
public abstract class TerrainMaterialMixin {
    @ModifyExpressionValue(method = {"fillChunk", "enforceOceanLayer"}, at = @At(value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"), require = 2)
    private static boolean emptyMaterial(boolean empty, @Local SkyIslandColumnMaterialPlan plan) {
        return empty && ((GeologicalMaterial)(Object)plan).createSky$water().sources().isEmpty();
    }

    @Inject(method = "fillChunk", at = @At("RETURN"))
    private static void fluidUpdates(ChunkAccess chunk, SkyIslandSettings settings, IslandField field,
            BlockState stone, BlockState water, CallbackInfo callback) {
        WaterPostprocessing.mark(chunk, field, settings);
    }
    @WrapOperation(method = {"fillChunk", "enforceOceanLayer", "baseHeightAt", "baseColumnAt"}, at = @At(value = "INVOKE",
            target = "Lorg/sathrek/sky_archipelago/worldgen/generator/terrain/SkyIslandColumnMaterialPlan;create(Ljava/util/List;IILorg/sathrek/sky_archipelago/config/SkyIslandSettings;IIJ)Lorg/sathrek/sky_archipelago/worldgen/generator/terrain/SkyIslandColumnMaterialPlan;"))
    private static SkyIslandColumnMaterialPlan material(List<TerrainColumn> segments, int minY, int maxY,
            SkyIslandSettings settings, int x, int z, long seed, Operation<SkyIslandColumnMaterialPlan> original,
            @Local(argsOnly = true) IslandField field) {
        var plan = original.call(segments, minY, maxY, settings, x, z, seed);
        ((GeologicalMaterial)(Object)plan).createSky$setFrame(((GeologicalField)(Object)field).createSky$frameAt(x, z, settings));
        ((GeologicalMaterial)(Object)plan).createSky$setVeins(((design.inherent.createskycompat.terrain.VeinField)(Object)field).createSky$veins());
        ((GeologicalMaterial)(Object)plan).createSky$setWater(((InteriorField)(Object)field).createSky$waterAt(x, z, settings));
        return plan;
    }
}

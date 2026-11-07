package design.inherent.skyweavecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import design.inherent.skyweavecompat.terrain.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.sathrek.sky_archipelago.worldgen.generator.field.TerrainColumn;
import org.sathrek.sky_archipelago.worldgen.generator.field.WaterArchetypeColumnProfile;
import org.sathrek.sky_archipelago.worldgen.generator.river.RiverColumnSample;
import org.sathrek.sky_archipelago.worldgen.generator.terrain.SkyIslandColumnMaterialPlan;
import org.sathrek.sky_archipelago.worldgen.generator.terrain.SkyIslandTerrainPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(value = SkyIslandTerrainPass.class, remap = false)
public abstract class TerrainMaterialMixin {
    @Inject(method = "fillChunk", at = @At("RETURN"))
    private static void fluidUpdates(ChunkAccess chunk, SkyIslandSettings settings, IslandField field,
            BlockState stone, BlockState oceanFluid, BlockState riverFluid, CallbackInfo callback) {
        WaterPostprocessing.mark(chunk, field, settings);
    }

    @WrapOperation(method = {"fillChunk", "enforceGlobalOceanLayer", "baseHeightAt", "baseColumnAt"},
            at = @At(value = "INVOKE", target = "Lorg/sathrek/sky_archipelago/worldgen/generator/terrain/SkyIslandColumnMaterialPlan;create(Ljava/util/List;IILorg/sathrek/sky_archipelago/config/SkyIslandSettings;IIJLorg/sathrek/sky_archipelago/worldgen/generator/river/RiverColumnSample;Lorg/sathrek/sky_archipelago/worldgen/generator/field/WaterArchetypeColumnProfile;)Lorg/sathrek/sky_archipelago/worldgen/generator/terrain/SkyIslandColumnMaterialPlan;"), require = 4)
    private static SkyIslandColumnMaterialPlan material(List<TerrainColumn> segments, int minY, int maxY,
            SkyIslandSettings settings, int x, int z, long seed, RiverColumnSample river,
            WaterArchetypeColumnProfile water, Operation<SkyIslandColumnMaterialPlan> original,
            @Local(argsOnly = true) IslandField field) {
        var plan = original.call(segments, minY, maxY, settings, x, z, seed, river, water);
        ((GeologicalMaterial)(Object)plan).skyweave$setFrame(((GeologicalField)(Object)field).skyweave$frameAt(x, z, settings));
        ((GeologicalMaterial)(Object)plan).skyweave$setVeins(((VeinField)(Object)field).skyweave$veins());
        return plan;
    }
}

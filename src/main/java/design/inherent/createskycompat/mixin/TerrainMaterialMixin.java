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
import java.util.List;

@Mixin(value = SkyIslandTerrainPass.class, remap = false)
public abstract class TerrainMaterialMixin {
    @WrapOperation(method = {"fillChunk", "enforceOceanLayer", "baseHeightAt", "baseColumnAt"}, at = @At(value = "INVOKE",
            target = "Lorg/sathrek/sky_archipelago/worldgen/generator/terrain/SkyIslandColumnMaterialPlan;create(Ljava/util/List;IILorg/sathrek/sky_archipelago/config/SkyIslandSettings;IIJ)Lorg/sathrek/sky_archipelago/worldgen/generator/terrain/SkyIslandColumnMaterialPlan;"))
    private static SkyIslandColumnMaterialPlan material(List<TerrainColumn> segments, int minY, int maxY,
            SkyIslandSettings settings, int x, int z, long seed, Operation<SkyIslandColumnMaterialPlan> original,
            @Local(argsOnly = true) IslandField field) {
        var plan = original.call(segments, minY, maxY, settings, x, z, seed);
        ((GeologicalMaterial)(Object)plan).createSky$setFrame(((GeologicalField)(Object)field).createSky$frameAt(x, z, settings));
        return plan;
    }
}

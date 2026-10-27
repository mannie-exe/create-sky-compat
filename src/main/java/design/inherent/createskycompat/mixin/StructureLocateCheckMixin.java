package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.GeologicalGenerator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.structure.StructurePlacementCategory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A native-source biome preview cannot rule out a structure that will be relocated. */
@Mixin(StructureCheck.class)
public abstract class StructureLocateCheckMixin {
    @Shadow @Final private ChunkGenerator chunkGenerator;
    @Shadow @Final private RegistryAccess registryAccess;

    @Inject(method = "canCreateStructure", at = @At("HEAD"), cancellable = true)
    private void islandPlacement(ChunkPos chunk, Structure structure, CallbackInfoReturnable<Boolean> result) {
        if (!(chunkGenerator instanceof SkyIslandChunkGenerator sky)
                || !((GeologicalGenerator)(Object)sky).createSky$isEnabled()) return;
        var id = registryAccess.registryOrThrow(Registries.STRUCTURE).getKey(structure);
        if (id == null) return;
        var settings = sky.resolvedSkyIslandSettings();
        var policy = settings.advanced().structurePlacementPolicy();
        // checkStart has already consulted loaded/saved starts and placement restrictions.
        // Returning true requests STRUCTURE_STARTS, not a successful start or a FULL chunk.
        result.setReturnValue(!policy.denylistedStructures().contains(id)
                && (policy.categoryFor(id) != StructurePlacementCategory.WATER || settings.terrain().ocean().oceanEnabled()));
    }
}

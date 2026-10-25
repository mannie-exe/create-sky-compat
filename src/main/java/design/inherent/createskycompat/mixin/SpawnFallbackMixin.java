package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.terrain.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.sathrek.sky_archipelago.worldgen.spawn.SkyIslandSpawnFallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SkyIslandSpawnFallback.class, remap = false)
public abstract class SpawnFallbackMixin {
    @Redirect(method = "onServerStarted", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setDefaultSpawnPos(Lnet/minecraft/core/BlockPos;F)V"))
    private static void generatedLanding(ServerLevel level, BlockPos position, float angle) {
        var generator = level.getChunkSource().getGenerator();
        if (generator instanceof GeologicalGenerator adapted && adapted.createSky$isEnabled()) {
            position = SpawnLanding.resolve(level, position);
        }
        level.setDefaultSpawnPos(position, angle);
    }
}

package design.inherent.createskycompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import design.inherent.createskycompat.climate.IslandClimate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class WeatherClimateMixin {
    @Shadow private ClientLevel level;
    @WrapOperation(method = {"renderSnowAndRain", "tickRain"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"), require = 2)
    private Biome.Precipitation weather(Biome biome, BlockPos position, Operation<Biome.Precipitation> original) {
        return original.call(biome, IslandClimate.temperaturePosition(level, position));
    }
}

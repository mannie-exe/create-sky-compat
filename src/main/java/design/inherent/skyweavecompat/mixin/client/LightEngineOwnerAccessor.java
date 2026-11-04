package design.inherent.skyweavecompat.mixin.client;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightEngine.class)
public interface LightEngineOwnerAccessor {
    @Accessor("chunkSource") LightChunkGetter skyweave$lightOwner();
}

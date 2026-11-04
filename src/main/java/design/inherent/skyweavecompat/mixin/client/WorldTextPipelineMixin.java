package design.inherent.skyweavecompat.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import design.inherent.skyweavecompat.client.WorldTextLighting;
import design.inherent.skyweavecompat.client.WorldTextRenderType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
public abstract class WorldTextPipelineMixin implements WorldTextLighting.Pipeline {
    @Unique private final Map<ShaderKey, ShaderInstance> skyweave$textShaders = new EnumMap<>(ShaderKey.class);
    @Unique private final LinkedHashMap<RenderType, RenderType> skyweave$textTypes = new LinkedHashMap<>(32, 0.75f, true);

    @Shadow private ShaderInstance createFallbackShader(String name, ShaderKey key) throws IOException {
        throw new AssertionError("Mixin shadow");
    }

    @Override public RenderType skyweave$textType(RenderType original) {
        RenderSystem.assertOnRenderThread();
        if (original instanceof WorldTextRenderType) return original;
        var wrapped = skyweave$textTypes.computeIfAbsent(original, WorldTextRenderType::new);
        if (skyweave$textTypes.size() > 256) skyweave$textTypes.pollFirstEntry();
        return wrapped;
    }

    @Override public ShaderInstance skyweave$textShader(ShaderKey key) {
        RenderSystem.assertOnRenderThread();
        return skyweave$textShaders.computeIfAbsent(key, selected -> {
            try { return createFallbackShader("skyweave_world_text_" + selected.name(), selected); }
            catch (IOException failure) { throw new IllegalStateException("Cannot create Iris world-text fallback: " + selected, failure); }
        });
    }

    @Inject(method = "destroy", at = @At("HEAD"), require = 1, allow = 1)
    private void skyweave$releaseTextReferences(CallbackInfo callback) {
        // Iris registers fallback shaders with loadedShaders and owns their destruction.
        skyweave$textTypes.clear();
        skyweave$textShaders.clear();
    }
}

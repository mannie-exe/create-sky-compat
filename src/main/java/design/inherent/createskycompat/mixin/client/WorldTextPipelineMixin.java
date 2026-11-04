package design.inherent.createskycompat.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import design.inherent.createskycompat.client.WorldTextLighting;
import design.inherent.createskycompat.client.WorldTextRenderType;
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
    @Unique private final Map<ShaderKey, ShaderInstance> createSky$textShaders = new EnumMap<>(ShaderKey.class);
    @Unique private final LinkedHashMap<RenderType, RenderType> createSky$textTypes = new LinkedHashMap<>(32, 0.75f, true);

    @Shadow private ShaderInstance createFallbackShader(String name, ShaderKey key) throws IOException {
        throw new AssertionError("Mixin shadow");
    }

    @Override public RenderType createSky$textType(RenderType original) {
        RenderSystem.assertOnRenderThread();
        if (original instanceof WorldTextRenderType) return original;
        var wrapped = createSky$textTypes.computeIfAbsent(original, WorldTextRenderType::new);
        if (createSky$textTypes.size() > 256) createSky$textTypes.pollFirstEntry();
        return wrapped;
    }

    @Override public ShaderInstance createSky$textShader(ShaderKey key) {
        RenderSystem.assertOnRenderThread();
        return createSky$textShaders.computeIfAbsent(key, selected -> {
            try { return createFallbackShader("create_sky_world_text_" + selected.name(), selected); }
            catch (IOException failure) { throw new IllegalStateException("Cannot create Iris world-text fallback: " + selected, failure); }
        });
    }

    @Inject(method = "destroy", at = @At("HEAD"), require = 1, allow = 1)
    private void createSky$releaseTextReferences(CallbackInfo callback) {
        // Iris registers fallback shaders with loadedShaders and owns their destruction.
        createSky$textTypes.clear();
        createSky$textShaders.clear();
    }
}

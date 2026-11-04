package design.inherent.createskycompat.client;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

/** Keeps selected world text separate until Iris actually draws its batch. */
public final class WorldTextLighting {
    public interface Pipeline {
        RenderType createSky$textType(RenderType original);
        ShaderInstance createSky$textShader(ShaderKey key);
    }

    private WorldTextLighting() {}

    private static IrisRenderingPipeline pipeline() {
        if (IrisApi.getInstance().isRenderingShadowPass()) return null;
        var current = Iris.getPipelineManager().getPipeline().orElse(null);
        return current instanceof IrisRenderingPipeline iris && iris.shouldOverrideShaders() ? iris : null;
    }

    public static MultiBufferSource wrap(MultiBufferSource original) {
        var current = pipeline();
        if (current == null) return original;
        var owner = (Pipeline) current;
        return type -> original.getBuffer(owner.createSky$textType(type));
    }

    public static ShaderInstance shader(ShaderMap map, ShaderKey key) {
        if (!WorldTextScope.active() || !textKey(key)) return null;
        var current = pipeline();
        if (current == null || current.getShaderMap() != map) return null;
        return ((Pipeline) current).createSky$textShader(key);
    }

    public static boolean textKey(ShaderKey key) {
        return key == ShaderKey.TEXT || key == ShaderKey.TEXT_INTENSITY || key == ShaderKey.TEXT_BG;
    }
}

package design.inherent.skyweavecompat.client;

import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;

/** Adds a dormant guard to terrain programs; only actual construct draw scopes enable it. */
public final class ConstructShader {
    private ConstructShader() {}
    public static void patch(ASTParser parser, TranslationUnit tree, Root root) {
        if (root.identifierIndex.has("gl_FragDepth")) {
            com.mojang.logging.LogUtils.getLogger().warn("Construct occlusion skips a program with custom fragment depth");
            return;
        }
        tree.parseAndInjectNodes(parser, ASTInjectionPoint.BEFORE_DECLARATIONS,
                "uniform int skyweaveConstructOcclusion;",
                "uniform mat4 skyweaveConstructInverse;",
                "uniform vec4 skyweaveConstructViewport;",
                "uniform sampler2D skyweaveOpaqueDepth;",
                "uniform mat4 skyweaveOpaqueInverse;");
        tree.prependMainFunctionBody(parser, """
                if (skyweaveConstructOcclusion != 0) {
                    vec2 swUv = (gl_FragCoord.xy - skyweaveConstructViewport.xy) / skyweaveConstructViewport.zw;
                    ivec2 swSize = textureSize(skyweaveOpaqueDepth, 0);
                    ivec2 swPixel = clamp(ivec2(swUv * vec2(swSize)), ivec2(0), swSize - 1);
                    float swDepth = texelFetch(skyweaveOpaqueDepth, swPixel, 0).r;
                    if (swDepth > 0.0 && swDepth < 1.0) {
                        float swOwnDepth = max(0.0, uintBitsToFloat(max(2u, floatBitsToUint(gl_FragCoord.z)) - 2u));
                        float swLodDepth = min(1.0, uintBitsToFloat(floatBitsToUint(swDepth) + 2u));
                        vec4 swOwn = skyweaveConstructInverse * vec4(swUv * 2.0 - 1.0, swOwnDepth * 2.0 - 1.0, 1.0);
                        vec4 swLod = skyweaveOpaqueInverse * vec4(swUv * 2.0 - 1.0, swLodDepth * 2.0 - 1.0, 1.0);
                        if (swOwn.w > 0.0 && swLod.w > 0.0 && -swOwn.z / swOwn.w > -swLod.z / swLod.w + 0.015625) discard;
                    }
                }
                """);
    }
}

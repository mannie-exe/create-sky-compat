package design.inherent.skyweavecompat.mixin.client;

import design.inherent.skyweavecompat.client.ConstructShader;
import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net.irisshaders.iris.pipeline.transform.transformer.VanillaCoreTransformer",
        "net.irisshaders.iris.pipeline.transform.transformer.VanillaTransformer"}, remap = false)
abstract class ConstructShaderMixin {
    @Inject(method = "transform", at = @At("TAIL"), require = 1)
    private static void skyweave$depth(ASTParser parser, TranslationUnit tree, Root root,
            VanillaParameters params, CallbackInfo ci) {
        if (params.type == PatchShaderType.FRAGMENT && params.hasChunkOffset)
            ConstructShader.patch(parser, tree, root);
    }
}

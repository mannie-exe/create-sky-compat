package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.client.ConstructShader;
import io.github.douira.glsl_transformer.ast.transform.SingleASTTransformer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstructShaderTest {
    @Test void augmentsParsedMainWithoutReplacingOriginalOutputs() {
        var transformer = new SingleASTTransformer<>();
        transformer.setTransformation((tree, root) -> ConstructShader.patch(transformer, tree, root));
        String source = "#version 330 core\nout vec4 color; void main(){ color = vec4(0.5); }";
        String result = transformer.transform(source);
        assertTrue(result.contains("skyweaveConstructOcclusion"));
        assertTrue(result.contains("texelFetch"));
        assertTrue(result.contains("discard"));
        assertTrue(result.replaceAll("\\s+", "").contains("color=vec4(0.5f)"), result);
        // The composed program must still parse, including the bitwise precision envelope.
        assertDoesNotThrow(() -> new SingleASTTransformer<>(SingleASTTransformer.IDENTITY_TRANSFORMATION).transform(result));
    }
    @Test void customFragmentDepthKeepsItsOriginalProgram() {
        var transformer = new SingleASTTransformer<>();
        transformer.setTransformation((tree, root) -> ConstructShader.patch(transformer, tree, root));
        String result = transformer.transform("#version 330 core\nvoid main(){ gl_FragDepth = 0.5; }");
        assertFalse(result.contains("skyweaveConstructOcclusion"));
        assertTrue(result.contains("gl_FragDepth"));
    }
}

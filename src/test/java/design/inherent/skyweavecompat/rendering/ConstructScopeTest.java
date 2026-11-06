package design.inherent.skyweavecompat.rendering;

import design.inherent.skyweavecompat.client.ConstructUniforms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstructScopeTest {
    @Test void immediateUploadClassificationSurvivesNestingAndClearsAfterFailure() {
        assertFalse(ConstructUniforms.isSingleDraw());
        assertThrows(IllegalStateException.class, () -> ConstructUniforms.singleDraw(() -> {
            assertTrue(ConstructUniforms.isSingleDraw());
            assertThrows(IllegalArgumentException.class, () -> ConstructUniforms.singleDraw(() -> {
                assertTrue(ConstructUniforms.isSingleDraw());
                throw new IllegalArgumentException();
            }));
            assertTrue(ConstructUniforms.isSingleDraw());
            throw new IllegalStateException();
        }));
        assertFalse(ConstructUniforms.isSingleDraw());
    }
}

package design.inherent.createskycompat.questlog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IconTextLayoutTest {
    @Test void centerShortTitleWithoutFractionalPixels() {
        assertEquals(new IconTextLayout(16, 0, 3), IconTextLayout.centered(16, 9));
    }
    @Test void centerIconBesideWrappedTitleAndHandleMissingIcon() {
        assertEquals(new IconTextLayout(27, 5, 0), IconTextLayout.centered(16, 27));
        assertEquals(new IconTextLayout(9, 4, 0), IconTextLayout.centered(0, 9));
    }
}

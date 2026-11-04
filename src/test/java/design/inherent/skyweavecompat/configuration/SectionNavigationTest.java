package design.inherent.skyweavecompat.configuration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SectionNavigationTest {
    private SectionNavigation document() {
        var navigation = new SectionNavigation();
        navigation.add("first", 0); navigation.add("middle", 300); navigation.add("last", 650);
        return navigation;
    }
    @Test void clampedJumpKeepsTheChosenShortSectionUntilTheUserMoves() {
        var navigation = document();
        assertEquals(400, navigation.jump("last", 400));
        assertEquals("last", navigation.active(400, 400));
        assertEquals("middle", navigation.active(380, 400));
        assertEquals("last", navigation.active(400, 400));
        assertEquals(0, navigation.jump("first", 400));
        assertEquals("first", navigation.active(0, 400));
    }
    @Test void bookmarkRestoresTheSectionAfterEarlierContentChanges() {
        var before = document();
        var bookmark = before.remember(345, 700);
        var after = new SectionNavigation();
        after.add("first", 0); after.add("middle", 500); after.add("last", 850);
        assertEquals(545, after.restore(bookmark, 900));
        assertEquals("middle", after.active(545, 900));
    }
    @Test void shortDocumentsAndMissingBookmarksRemainBounded() {
        var navigation = document();
        assertEquals(0, navigation.jump("last", 0));
        assertEquals("last", navigation.active(0, 0));
        assertEquals(0, navigation.restore(new SectionNavigation.Bookmark("removed", 10), 100));
        assertEquals(100, navigation.restore(new SectionNavigation.Bookmark("middle", 500), 100));
        assertThrows(IllegalArgumentException.class, () -> navigation.add("first", 10));
        assertThrows(IllegalArgumentException.class, () -> navigation.jump("missing", 100));
    }
}

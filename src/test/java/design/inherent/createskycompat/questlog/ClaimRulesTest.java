package design.inherent.createskycompat.questlog;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClaimRulesTest {
    @Test void claimsRequireCompletionAndAvailability() {
        assertFalse(ClaimRules.eligible(true, false, false, 0, 1));
        assertFalse(ClaimRules.eligible(false, true, false, 0, 1));
        assertFalse(ClaimRules.eligible(true, true, true, 0, 1));
        assertFalse(ClaimRules.eligible(true, true, false, -1, 1));
        assertFalse(ClaimRules.eligible(true, true, false, 1, 1));
        assertTrue(ClaimRules.eligible(true, true, false, 1, 2));
    }

    @Test void choiceRequestsMustBeExactUniqueAndInRange() {
        assertTrue(ClaimRules.selections(List.of(2, 0), 3, 2));
        assertFalse(ClaimRules.selections(List.of(0, 0), 3, 2));
        assertFalse(ClaimRules.selections(List.of(0, 1, 2), 3, 2));
        assertFalse(ClaimRules.selections(List.of(0), 3, 2));
        assertFalse(ClaimRules.selections(List.of(-1), 3, 1));
        assertFalse(ClaimRules.selections(List.of(3), 3, 1));
        assertFalse(ClaimRules.selections(null, 3, 1));
        assertTrue(ClaimRules.selections(List.of(), 0, 0));
        assertFalse(ClaimRules.selections(List.of(0), 0, 0));
    }
}

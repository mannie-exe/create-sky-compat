package design.inherent.createskycompat.questlog;

import java.util.HashSet;
import java.util.List;

/** Validation before the upstream handler can mutate a selection or grant a reward. */
public final class ClaimRules {
    private ClaimRules() {}

    public static boolean eligible(boolean triggered, boolean completed, boolean rewarded, int index, int count) {
        return triggered && completed && !rewarded && index >= 0 && index < count;
    }

    public static boolean selections(List<Integer> indices, int choiceCount, int pickCount) {
        if (indices == null || pickCount < 0 || pickCount > choiceCount || indices.size() != pickCount) return false;
        var seen = new HashSet<Integer>();
        for (Integer index : indices)
            if (index == null || index < 0 || index >= choiceCount || !seen.add(index)) return false;
        return true;
    }
}

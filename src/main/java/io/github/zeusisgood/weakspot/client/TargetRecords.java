package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.TargetRules;
import java.util.ArrayList;
import java.util.List;

/**
 * 自分の的当ての記録と、サーバー内の上位（1.11.0。サーバーから届いた値）。ご褒美の解放（MarkerLook・KindsTab）と、
 * 統計画面の「的当て」タブが読む。記録はワールドごとなので、ワールドに入るたびにサーバーが送り直す。
 */
final class TargetRecords {

    static long best;
    static long rounds;
    static final List<String> TOP_NAMES = new ArrayList<>();
    static final List<Long> TOP_SCORES = new ArrayList<>();

    private TargetRecords() {
    }

    static void receive(long newBest, long newRounds, List<String> names, List<Long> scores) {
        best = newBest;
        rounds = newRounds;
        TOP_NAMES.clear();
        TOP_NAMES.addAll(names);
        TOP_SCORES.clear();
        TOP_SCORES.addAll(scores);
    }

    static TargetRules.Tier tier() {
        return TargetRules.Tier.of(best);
    }

    /** そのご褒美を使えるか。 */
    static boolean unlocked(TargetRules.Tier needed) {
        return tier().atLeast(needed);
    }
}

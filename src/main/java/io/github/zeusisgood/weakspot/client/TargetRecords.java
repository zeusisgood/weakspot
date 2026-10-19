package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.TargetRules;
import java.util.ArrayList;
import java.util.List;

/** 記録はワールドごとなので、ワールドに入るたびにサーバーが送り直す。 */
final class TargetRecords {

    static long best;
    static long rounds;
    /** しきい値を上げる前に解放した段階を残す。ログインのときサーバーが送る。 */
    static TargetRules.Tier earned = TargetRules.Tier.NONE;
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

    /** 自己ベストの段階と、取った進捗の段階の高いほう。 */
    static TargetRules.Tier tier() {
        TargetRules.Tier byBest = TargetRules.Tier.of(best);
        return earned.atLeast(byBest) ? earned : byBest;
    }

    static boolean unlocked(TargetRules.Tier needed) {
        return tier().atLeast(needed);
    }
}

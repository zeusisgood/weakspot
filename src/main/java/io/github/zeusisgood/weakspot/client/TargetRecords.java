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
    /** 取った進捗の段階（1.11.1。しきい値を上げる前に解放した段階を残す。ログインのときサーバーが送る）。 */
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

    /** ご褒美の段階（自己ベストの段階と、取った進捗の段階の高いほう）。 */
    static TargetRules.Tier tier() {
        TargetRules.Tier byBest = TargetRules.Tier.of(best);
        return earned.atLeast(byBest) ? earned : byBest;
    }

    /** そのご褒美を使えるか。 */
    static boolean unlocked(TargetRules.Tier needed) {
        return tier().atLeast(needed);
    }
}

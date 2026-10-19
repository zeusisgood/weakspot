package io.github.zeusisgood.weakspot.client;

import java.util.HashMap;
import java.util.Map;

/** 覚えるのは最後の相手だけ（perTarget が false）か、相手ごと（true。動物）。 */
final class QueryThrottle<K> {

    private final int intervalTicks;
    private final boolean perTarget;
    private final Map<K, Long> last = new HashMap<>();

    QueryThrottle(int intervalTicks, boolean perTarget) {
        this.intervalTicks = intervalTicks;
        this.perTarget = perTarget;
    }

    /** よければ、この tick を記録する。 */
    boolean due(K target, long tick, boolean force) {
        Long previous = last.get(target);
        if (!force && previous != null && tick - previous < intervalTicks) {
            return false;
        }
        if (!perTarget) {
            last.clear();
        }
        last.put(target, tick);
        return true;
    }

    /** 新しい浮き、ワールドを出たとき。 */
    void reset() {
        last.clear();
    }
}

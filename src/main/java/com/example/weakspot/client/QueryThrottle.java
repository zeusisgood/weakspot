package com.example.weakspot.client;

import java.util.HashMap;
import java.util.Map;

/**
 * サーバーへの問い合わせの間隔（1.8.8 で AnimalStates・MachineBars・FishingSpot から切り出した）。相手ごとに最後に
 * 問い合わせた tick を覚え、間隔があいていれば問い合わせてよいと答えて記録する（ヒットのときは force ですぐに）。
 * 覚えるのは最後の相手だけ（perTarget が false）か、相手ごと（true。動物）。
 */
final class QueryThrottle<K> {

    private final int intervalTicks;
    private final boolean perTarget;
    private final Map<K, Long> last = new HashMap<>();

    QueryThrottle(int intervalTicks, boolean perTarget) {
        this.intervalTicks = intervalTicks;
        this.perTarget = perTarget;
    }

    /** target に問い合わせてよいか。よければ、この tick を記録する。 */
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

    /** 次の問い合わせを、すぐにしてよいことにする（新しい浮き、ワールドを出たとき）。 */
    void reset() {
        last.clear();
    }
}

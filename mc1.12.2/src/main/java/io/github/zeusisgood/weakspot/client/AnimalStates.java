package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.AnimalTimers;
import io.github.zeusisgood.weakspot.common.QueryThrottle;
import io.github.zeusisgood.weakspot.network.QueryMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * 動物のタイマーはクライアントに届いていない（子どもの成長は getGrowingAge が ±1 しか返さない、卵のタイマーと村人の
 * 取引のロックは同期されない）ので、サーバーに問い合わせて覚える。
 */
final class AnimalStates {

    static final int QUERY_INTERVAL_TICKS = 5;
    /** これより前の返事は古いので使わない（返事が来なければ弱点を出さない）。 */
    private static final int STALE_TICKS = 30;

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final QueryThrottle<Integer> QUERIES = new QueryThrottle<>(QUERY_INTERVAL_TICKS, true);

    private AnimalStates() {
    }

    static final class State {
        final int mask;
        final float[] progress;
        final long tick;

        State(int mask, float[] progress, long tick) {
            this.mask = mask;
            this.progress = progress;
            this.tick = tick;
        }

        /** 動いているタイマーがなければ -1。 */
        double barProgress() {
            AnimalTimers.Timer timer = AnimalTimers.barTimer(mask);
            return timer == null ? -1 : progress[timer.ordinal()];
        }
    }

    /** force ならすぐに送る。 */
    static void query(int entityId, long tick, boolean force) {
        if (!QUERIES.due(entityId, tick, force)) {
            return;
        }
        WeakSpotMod.network.sendToServer(QueryMessage.animal(entityId));
    }

    /** クライアントのスレッドで呼ぶ。 */
    static void receive(int entityId, int mask, float[] progress) {
        STATES.put(entityId, new State(mask, progress.clone(), ClientWeakSpotHandler.clientTick));
    }

    /** なければ null。 */
    static State get(int entityId, long tick) {
        State state = STATES.get(entityId);
        return state != null && tick - state.tick <= STALE_TICKS ? state : null;
    }

    /** バニラの右クリックの動作を止めるのに使う。 */
    static boolean isActive(int entityId) {
        State state = get(entityId, ClientWeakSpotHandler.clientTick);
        return state != null && state.mask != 0;
    }

    static void clear() {
        STATES.clear();
        QUERIES.reset();
    }
}

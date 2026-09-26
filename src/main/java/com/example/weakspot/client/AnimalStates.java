package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.AnimalTimers;
import com.example.weakspot.network.QueryMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * サーバーから届いた動物の状態（動いているタイマーと進み具合）を覚える。動物のタイマーはクライアントに届いていない
 * （子どもの成長は getGrowingAge が ±1 しか返さない、卵のタイマーと村人の取引のロックは同期されない）ので、
 * 右クリックを押しっぱなしで動物に照準を合わせている間、数 tick ごとと、ヒットのたびに、サーバーに問い合わせる。
 */
final class AnimalStates {

    /** 問い合わせの間隔（tick）。 */
    static final int QUERY_INTERVAL_TICKS = 5;
    /** この tick 以上前の返事は、古いので使わない（返事が来なければ弱点を出さない）。 */
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

        /** 足元のバーに出す進み具合（表示の順番で先のタイマー）。動いているタイマーがなければ -1。 */
        double barProgress() {
            AnimalTimers.Timer timer = AnimalTimers.barTimer(mask);
            return timer == null ? -1 : progress[timer.ordinal()];
        }
    }

    /** 動物の状態を問い合わせる。前の問い合わせから間隔があいていなければ送らない（force ならすぐに送る）。 */
    static void query(int entityId, long tick, boolean force) {
        if (!QUERIES.due(entityId, tick, force)) {
            return;
        }
        WeakSpotMod.network.sendToServer(QueryMessage.animal(entityId));
    }

    /** サーバーの返事が届いた（クライアントのスレッドで呼ぶ）。 */
    static void receive(int entityId, int mask, float[] progress) {
        STATES.put(entityId, new State(mask, progress.clone(), ClientWeakSpotHandler.clientTick));
    }

    /** 新しい返事（なければ null）。 */
    static State get(int entityId, long tick) {
        State state = STATES.get(entityId);
        return state != null && tick - state.tick <= STALE_TICKS ? state : null;
    }

    /** サーバーの返事を覚えていて、動いているタイマーがあるか（バニラの右クリックの動作を止めるのに使う）。 */
    static boolean isActive(int entityId) {
        State state = get(entityId, ClientWeakSpotHandler.clientTick);
        return state != null && state.mask != 0;
    }

    static void clear() {
        STATES.clear();
        QUERIES.reset();
    }
}

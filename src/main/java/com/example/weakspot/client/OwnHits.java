package com.example.weakspot.client;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.HitStreak;
import java.util.Arrays;

/**
 * 自分のヒットの共通の処理（1.8.9 で ClientWeakSpotHandler から分けた）: 連続ヒット（コンボ）、ヒット音、コンボの表示、
 * 種類ごとのヒット間隔。すべての種類の弱点が、当てたときに register を呼ぶ。
 */
final class OwnHits {

    /** 種類ごとの最後のヒット（ヒット間隔の制限に使う）。 */
    private static final long[] LAST_HIT_TICK = new long[HitKind.values().length];
    /** 連続ヒット数。ブロックや種類をまたいで続き、ヒット音のピッチとコンボの表示に使う。 */
    static final HitStreak STREAK = new HitStreak();

    static {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    private OwnHits() {
    }

    /** 前のヒットから minInterval tick あいているか。 */
    static boolean canHit(HitKind kind, int minInterval) {
        return ClientWeakSpotHandler.clientTick - LAST_HIT_TICK[kind.ordinal()] >= minInterval;
    }

    /**
     * どの種類のヒットにも共通の処理: 連続ヒット、ヒット音、コンボの表示、ヒット間隔の記録。ヒット後の連続ヒット数を返す。
     */
    static int register(HitKind kind) {
        int hitStreak = STREAK.hit(ClientWeakSpotHandler.clientTick);
        HitSounds.playOwn(hitStreak);
        ComboHud.onHit(kind, hitStreak, ClientWeakSpotHandler.clientTick + ClientWeakSpotHandler.framePartialTicks);
        LAST_HIT_TICK[kind.ordinal()] = ClientWeakSpotHandler.clientTick;
        return hitStreak;
    }

    /** ワールドを出たとき。 */
    static void clearIntervals() {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    /** 連続ヒットを最初に戻す（死亡・リスポーン・ディメンション移動・ワールドを出たとき）。 */
    static void resetStreak() {
        STREAK.reset();
        ComboHud.clear();
    }
}

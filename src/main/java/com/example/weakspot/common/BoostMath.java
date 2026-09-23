package com.example.weakspot.common;

/**
 * 破壊速度ブーストの計算。
 *
 * 1.12.2 のサーバーは破壊進捗を積算せず、「現在の破壊速度 × (経過tick + 1)」で破壊完了を判定する
 * （PlayerInteractionManager#blockRemoving / updateBlockRemoving）。そのためサーバーでは
 * ヒットで得た追加進捗を「通常速度で何tick分か」として貯めておき、判定時の速度に
 * (経過tick + 1 + 追加tick) / (経過tick + 1) を掛けて、積算した場合と同じ結果にする。
 */
public final class BoostMath {

    private BoostMath() {
    }

    /** 1回のヒットで得る追加進捗（通常速度のtick数）。(倍率 − 1) × 継続時間。 */
    public static double extraTicksPerHit(double multiplier, int durationTicks) {
        return Math.max(0, multiplier - 1) * Math.max(0, durationTicks);
    }

    /** サーバー側で破壊速度に掛ける係数。 */
    public static double serverSpeedFactor(double extraTicks, long elapsedTicks) {
        if (extraTicks <= 0) {
            return 1;
        }
        return 1 + extraTicks / (Math.max(0, elapsedTicks) + 1);
    }
}

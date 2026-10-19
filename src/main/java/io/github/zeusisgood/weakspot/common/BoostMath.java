package io.github.zeusisgood.weakspot.common;

/**
* 1.12.2 のサーバーは破壊進捗を積算せず、「現在の破壊速度 × (経過tick + 1)」で破壊完了を判定する
* （PlayerInteractionManager#blockRemoving / updateBlockRemoving）。そのためサーバーでは
* ヒットで得た追加進捗を「通常速度で何tick分か」として貯めておき、判定時の速度に
* (経過tick + 1 + 追加tick) / (経過tick + 1) を掛けて、積算した場合と同じ結果にする。
*/
public final class BoostMath {

    /** クライアントとサーバーで同じ。 */
    public static final int PRE_DIG_TICKS = 20;

    private BoostMath() {
    }

    /** 通常速度の tick を返す。 */
    public static double extraTicksPerHit(double multiplier, int durationTicks) {
        return Math.max(0, multiplier - 1) * Math.max(0, durationTicks);
    }

    public static double extraTicksPerHit(double multiplier, int durationTicks, double comboFactor) {
        return extraTicksPerHit(multiplier, durationTicks) * Math.max(0, comboFactor);
    }

    /** クライアントで、時間枠の中の破壊速度に掛ける。 */
    public static double clientMultiplier(double multiplier, double comboFactor) {
        return 1 + Math.max(0, multiplier - 1) * Math.max(0, comboFactor);
    }

    public static double serverSpeedFactor(double extraTicks, long elapsedTicks) {
        if (extraTicks <= 0) {
            return 1;
        }
        return 1 + extraTicks / (Math.max(0, elapsedTicks) + 1);
    }
}

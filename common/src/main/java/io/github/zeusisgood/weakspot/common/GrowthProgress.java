package io.github.zeusisgood.weakspot.common;

public final class GrowthProgress {

    private GrowthProgress() {
    }

    /** 範囲外は 0〜1 に収める。最大が 0 以下なら 0。 */
    public static double fraction(int value, int max) {
        if (max <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) value / max));
    }
}

package io.github.zeusisgood.weakspot.common;

/** 作物の成長バーに出す進み具合（0.0〜1.0）。Minecraft に依存しない。 */
public final class GrowthProgress {

    private GrowthProgress() {
    }

    /** 段階 ÷ 最大の段階。範囲外は 0.0〜1.0 に収める。最大が 0 以下なら 0.0。 */
    public static double fraction(int value, int max) {
        if (max <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) value / max));
    }
}

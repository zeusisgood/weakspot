package com.example.weakspot.common;

/** 作物の成長バーに出す進み具合（0.0〜1.0）。Minecraft に依存しない。 */
public final class GrowthProgress {

    /** サトウキビ・サボテンの年齢の最大（次の節が伸びるまでの進み具合を、年齢 ÷ 15 で表す）。 */
    public static final int COLUMN_MAX_AGE = 15;

    private GrowthProgress() {
    }

    /** 段階 ÷ 最大の段階。範囲外は 0.0〜1.0 に収める。最大が 0 以下なら 0.0。 */
    public static double fraction(int value, int max) {
        if (max <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) value / max));
    }

    /** サトウキビ・サボテンの、柱の一番上の節の年齢から。 */
    public static double column(int age) {
        return fraction(age, COLUMN_MAX_AGE);
    }
}

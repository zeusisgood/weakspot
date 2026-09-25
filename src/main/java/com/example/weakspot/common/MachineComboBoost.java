package com.example.weakspot.common;

/**
 * コンボ（連続ヒット数）に応じた機械の加速の掛け数（1.4.2）。機械の倍率 = min(machineBoostMultiplier × 掛け数, 上限)。
 * サーバーはこれで倍率を決め、クライアントは「機械 ×n」の表示に使う（掛け数だけ。サーバーの倍率は届いていない）。
 */
public final class MachineComboBoost {

    /** この数以上のコンボで、同じ位置の掛け数になる。 */
    private static final int[] STEPS = {25, 50, 100, 250, 500, 1000};
    private static final double[] FACTORS = {1.25, 1.5, 2.0, 2.5, 3.0, 4.0};

    private MachineComboBoost() {
    }

    /** コンボの掛け数（24 以下は 1.0）。 */
    public static double factor(int combo) {
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (combo >= STEPS[i]) {
                return FACTORS[i];
            }
        }
        return 1.0;
    }

    /** 機械の倍率。上限が base より小さくても、上限を優先する。 */
    public static double multiplier(double base, double max, int combo) {
        return Math.min(base * factor(combo), max);
    }

    /** 表示用の掛け数（2.0 → "2.0"、2.5 → "2.5"、1.25 → "1.25"）。 */
    public static String label(double factor) {
        return String.valueOf(factor);
    }

    /** 表示用の速さ（1.6.0。整数なら整数で「4」、そうでなければ小数 1 桁で「7.5」）。 */
    public static String speedLabel(double speed) {
        long rounded = Math.round(speed);
        if (Math.abs(speed - rounded) < 1e-9) {
            return Long.toString(rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", speed);
    }
}

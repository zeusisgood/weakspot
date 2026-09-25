package com.example.weakspot.common;

/**
 * コンボ（連続ヒット数）に応じた機械の加速（1.4.2）。掛け数は ComboFactor（1.8.6 で切り出した）。機械の倍率 = min(machineBoostMultiplier × 掛け数, 上限)。
 * サーバーはこれで倍率を決め、クライアントは「機械 ×n」の表示に使う（掛け数だけ。サーバーの倍率は届いていない）。
 */
public final class MachineComboBoost {

    private MachineComboBoost() {
    }

    /** 機械の倍率。上限が base より小さくても、上限を優先する。 */
    public static double multiplier(double base, double max, int combo) {
        return Math.min(base * ComboFactor.factor(combo), max);
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

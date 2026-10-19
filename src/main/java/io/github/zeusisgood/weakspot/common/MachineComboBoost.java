package io.github.zeusisgood.weakspot.common;

/** クライアントは「機械 ×n」の表示に使う（サーバーの倍率は届いていないので、掛け数から計算する）。 */
public final class MachineComboBoost {

    private MachineComboBoost() {
    }

    /** 上限が base より小さくても、上限を優先する。 */
    public static double multiplier(double base, double max, int combo) {
        return Math.min(base * ComboFactor.factor(combo), max);
    }

    /** 整数なら「4」、そうでなければ小数 1 桁で「7.5」。 */
    public static String speedLabel(double speed) {
        long rounded = Math.round(speed);
        if (Math.abs(speed - rounded) < 1e-9) {
            return Long.toString(rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", speed);
    }
}

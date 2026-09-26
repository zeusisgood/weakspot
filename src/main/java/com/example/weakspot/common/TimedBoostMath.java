package com.example.weakspot.common;

/**
 * 時間で続く加速（乗り物・はしご・ダッシュ）の倍率（1.6.0。1.8.9 で VehicleBoostMath から改名）。
 * 基本の倍率（vehicleBoostMultiplier など）× コンボの掛け数（ComboFactor.factor）。
 * 上限 max は 0 以下なら無し（初期値。ユーザーの判断）。
 */
public final class TimedBoostMath {

    private TimedBoostMath() {
    }

    public static double multiplier(double base, double max, int combo) {
        double value = base * ComboFactor.factor(combo);
        return max > 0 ? Math.min(value, max) : value;
    }

    /** 余分に進める割合（倍率 − 1。1 より小さい倍率は 0）。 */
    public static double extra(double multiplier) {
        return Math.max(0, multiplier - 1);
    }
}

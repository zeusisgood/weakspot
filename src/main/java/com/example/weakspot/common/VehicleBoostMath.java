package com.example.weakspot.common;

/**
 * 乗り物の加速の倍率（1.6.0）。vehicleBoostMultiplier × コンボの掛け数（MachineComboBoost.factor）。
 * 上限 max は 0 以下なら無し（初期値。ユーザーの判断）。
 */
public final class VehicleBoostMath {

    private VehicleBoostMath() {
    }

    public static double multiplier(double base, double max, int combo) {
        double value = base * MachineComboBoost.factor(combo);
        return max > 0 ? Math.min(value, max) : value;
    }

    /** 余分に進める割合（倍率 − 1。1 より小さい倍率は 0）。 */
    public static double extra(double multiplier) {
        return Math.max(0, multiplier - 1);
    }
}

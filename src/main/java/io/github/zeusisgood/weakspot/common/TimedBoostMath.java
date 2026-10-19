package io.github.zeusisgood.weakspot.common;

/** 上限 max は 0 以下なら無し（初期値。速さの上限は付けない方針）。 */
public final class TimedBoostMath {

    private TimedBoostMath() {
    }

    public static double multiplier(double base, double max, int combo) {
        double value = base * ComboFactor.factor(combo);
        return max > 0 ? Math.min(value, max) : value;
    }

    /** 1 より小さい倍率は 0。 */
    public static double extra(double multiplier) {
        return Math.max(0, multiplier - 1);
    }
}

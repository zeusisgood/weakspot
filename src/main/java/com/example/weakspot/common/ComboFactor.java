package com.example.weakspot.common;

/**
 * コンボ（連続ヒット数）に応じた掛け数（1.4.2 で機械の加速に入れ、1.6.0 から乗り物・投げる物などでも使う）。
 * 1.8.6 で MachineComboBoost から切り出した。
 */
public final class ComboFactor {

    /** この数以上のコンボで、同じ位置の掛け数になる。 */
    private static final int[] STEPS = {25, 50, 100, 250, 500, 1000};
    private static final double[] FACTORS = {1.25, 1.5, 2.0, 2.5, 3.0, 4.0};

    private ComboFactor() {
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
}

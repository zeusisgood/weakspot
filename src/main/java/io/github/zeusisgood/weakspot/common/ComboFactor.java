package io.github.zeusisgood.weakspot.common;

/**
 * コンボ（連続ヒット数）に応じた掛け数（1.4.2 で機械の加速に入れ、1.6.0 から乗り物・投げる物などでも使う）。
 * 1.8.6 で MachineComboBoost から切り出した。1.11.0 から段階を 300 までに詰め、その先は 100 ごとに 0.5 ずつ上がり続ける（上限なし）。
 */
public final class ComboFactor {

    /** この数以上のコンボで、同じ位置の掛け数になる（300 まで）。 */
    private static final int[] STEPS = {25, 50, 100, 150, 200, 250, 300};
    private static final double[] FACTORS = {1.25, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0};
    /** 最後の表の段階より先は、この数ごとに FACTOR_PER_REPEAT ずつ上がる。 */
    public static final int REPEAT_STEP = 100;
    private static final double FACTOR_PER_REPEAT = 0.5;

    private ComboFactor() {
    }

    private static int lastStep() {
        return STEPS[STEPS.length - 1];
    }

    /** コンボの掛け数（24 以下は 1.0。300 で 4.0、その先は 100 ごとに +0.5）。 */
    public static double factor(int combo) {
        if (combo >= lastStep()) {
            return FACTORS[FACTORS.length - 1] + FACTOR_PER_REPEAT * ((combo - lastStep()) / REPEAT_STEP);
        }
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (combo >= STEPS[i]) {
                return FACTORS[i];
            }
        }
        return 1.0;
    }

    /** 次に掛け数が上がるコンボ（1.10.2。1.11.0 から終わりはない）。 */
    public static int nextStep(int combo) {
        for (int step : STEPS) {
            if (combo < step) {
                return step;
            }
        }
        return lastStep() + ((combo - lastStep()) / REPEAT_STEP + 1) * REPEAT_STEP;
    }

    /** 今の段階（最初は 0）。 */
    private static int previousStep(int combo) {
        if (combo >= lastStep()) {
            return lastStep() + ((combo - lastStep()) / REPEAT_STEP) * REPEAT_STEP;
        }
        int from = 0;
        for (int step : STEPS) {
            if (combo >= step) {
                from = step;
            }
        }
        return from;
    }

    /** 前の段階（最初は 0）から次の段階までの進み具合（0 以上 1 未満。1.10.2）。 */
    public static double progressToNext(int combo) {
        int from = previousStep(Math.max(0, combo));
        int to = nextStep(Math.max(0, combo));
        return Math.max(0, combo - from) / (double) (to - from);
    }
}

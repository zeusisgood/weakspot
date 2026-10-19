package io.github.zeusisgood.weakspot.common;

/** 段階は 300 まで、その先は 100 ごとに 0.5 ずつ上がり続ける（上限なし）。 */
public final class ComboFactor {

    private static final int[] STEPS = {25, 50, 100, 150, 200, 250, 300};
    private static final double[] FACTORS = {1.25, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0};
    public static final int REPEAT_STEP = 100;
    private static final double FACTOR_PER_REPEAT = 0.5;

    private ComboFactor() {
    }

    private static int lastStep() {
        return STEPS[STEPS.length - 1];
    }

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

    public static int nextStep(int combo) {
        for (int step : STEPS) {
            if (combo < step) {
                return step;
            }
        }
        return lastStep() + ((combo - lastStep()) / REPEAT_STEP + 1) * REPEAT_STEP;
    }

    /** 最初は 0。 */
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

    /** 0 以上 1 未満。 */
    public static double progressToNext(int combo) {
        int from = previousStep(Math.max(0, combo));
        int to = nextStep(Math.max(0, combo));
        return Math.max(0, combo - from) / (double) (to - from);
    }
}

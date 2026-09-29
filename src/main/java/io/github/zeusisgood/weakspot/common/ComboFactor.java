package io.github.zeusisgood.weakspot.common;

/**
 * コンボ（連続ヒット数）に応じた掛け数（1.4.2 で機械の加速に入れ、1.6.0 から乗り物・投げる物などでも使う）。
 * 1.8.6 で MachineComboBoost から切り出した。
 */
public final class ComboFactor {

    /** この数以上のコンボで、同じ位置の掛け数になる。 */
    private static final int[] STEPS = {25, 50, 100, 250, 500, 1000};
    private static final double[] FACTORS = {1.25, 1.5, 2.0, 2.5, 3.0, 4.0};
    /** 次の段階までのゲージの色（1.10.2。その段階の色: オレンジ・赤・紫・ピンク・水色・金）。 */
    private static final int[] STEP_RGB = {0xFFAA00, 0xFF5555, 0xAA55FF, 0xFF55FF, 0x55FFFF, 0xFFD700};

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

    /** combo の次の段階の番号（STEPS の位置）。最後の段階に届いていれば -1。 */
    private static int nextIndex(int combo) {
        for (int i = 0; i < STEPS.length; i++) {
            if (combo < STEPS[i]) {
                return i;
            }
        }
        return -1;
    }

    /** 次に掛け数が上がるコンボ（1.10.2）。最後の段階に届いていれば -1。 */
    public static int nextStep(int combo) {
        int i = nextIndex(combo);
        return i < 0 ? -1 : STEPS[i];
    }

    /** 前の段階（最初は 0）から次の段階までの進み具合（0 以上 1 未満。1.10.2）。最後の段階に届いていれば -1。 */
    public static double progressToNext(int combo) {
        int i = nextIndex(combo);
        if (i < 0) {
            return -1;
        }
        int from = i == 0 ? 0 : STEPS[i - 1];
        return Math.max(0, combo - from) / (double) (STEPS[i] - from);
    }

    /** 次の段階の色（0xRRGGBB。1.10.2）。最後の段階に届いていれば -1。 */
    public static int nextStepRgb(int combo) {
        int i = nextIndex(combo);
        return i < 0 ? -1 : STEP_RGB[i];
    }
}

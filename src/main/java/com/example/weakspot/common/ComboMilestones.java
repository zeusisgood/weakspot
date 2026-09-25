package com.example.weakspot.common;

/**
 * コンボの段階の演出（10、25、50、100、250、500、1000、以降 1000 ごと）を出すかの判定。その数に達した瞬間だけ真になる。
 * 累計ヒットの節目（Milestones）とは別物で、その連続の中だけのもの。
 */
public final class ComboMilestones {

    private static final int[] STEPS = {10, 25, 50, 100, 250, 500, 1000};
    /** 1000 より上は、この数ごとに 1000 と同じ演出を出す。 */
    public static final int GRAND_STEP = 1000;

    private int lastCombo;

    /** ヒットのたびに今のコンボを渡す。段階の数に達した瞬間なら true（同じ数を続けて渡しても1回だけ）。 */
    public boolean reached(int combo) {
        boolean changed = combo != lastCombo;
        lastCombo = combo;
        return changed && isStep(combo);
    }

    public void reset() {
        lastCombo = 0;
    }

    static boolean isStep(int combo) {
        if (combo > GRAND_STEP) {
            return combo % GRAND_STEP == 0;
        }
        for (int step : STEPS) {
            if (combo == step) {
                return true;
            }
        }
        return false;
    }

    /**
     * 段階の演出の光の色（250 はピンク、500 は水色、1000 以上は金）。それより下の段階は -1（数字の色を使う）。
     */
    public static int glowRgb(int step) {
        if (step >= 1000) {
            return 0xFFD700;
        }
        if (step >= 500) {
            return 0x55FFFF;
        }
        if (step >= 250) {
            return 0xFF55FF;
        }
        return -1;
    }
}

package com.example.weakspot.common;

/**
 * コンボの段階の演出（10、25、50、100）を出すかの判定。その数に達した瞬間だけ真になる。
 * 累計ヒットの節目（Milestones）とは別物で、その連続の中だけのもの。
 */
public final class ComboMilestones {

    private static final int[] STEPS = {10, 25, 50, 100};

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
        for (int step : STEPS) {
            if (combo == step) {
                return true;
            }
        }
        return false;
    }
}

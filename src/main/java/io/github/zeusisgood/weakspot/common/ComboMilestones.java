package io.github.zeusisgood.weakspot.common;

/**
 * コンボの段階の演出（10、25、50、75、100、150、200、250、300、以降 100 ごと。1.11.0 で 300 までに詰めた）を出すかの判定。
 * その数に達した瞬間だけ真になる。累計ヒットの節目（Milestones）とは別物で、その連続の中だけのもの。
 */
public final class ComboMilestones {

    private static final int[] STEPS = {10, 25, 50, 75, 100, 150, 200, 250, 300};
    /** 300 より上は、この数ごとに段階の演出を出す。 */
    public static final int REPEAT_STEP = 100;
    /** この数ごとに、いちばん大きな演出（2 オクターブ・花火 3 発）を出す。 */
    public static final int GRAND_STEP = 500;
    /** タイトル「N COMBO!」を出す最初の段階。 */
    public static final int TITLE_FROM = 300;
    /** 強い光と大きな弾みを出す最初の段階。 */
    public static final int STRONG_FROM = 150;

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

    /** 段階の数か（10・25・50・75・100・150・200・250・300、以降 100 ごと）。全員への知らせ（サーバー）も使う。 */
    public static boolean isStep(int combo) {
        int last = STEPS[STEPS.length - 1];
        if (combo > last) {
            return combo % REPEAT_STEP == 0;
        }
        for (int step : STEPS) {
            if (combo == step) {
                return true;
            }
        }
        return false;
    }

    /**
     * 段階の演出の光の色（150 から段階の色、500 ごとは金）。それより下と、虹色の段階（400 から）は -1（数字の色を使う）。
     */
    public static int glowRgb(int step) {
        if (step >= GRAND_STEP && step % GRAND_STEP == 0) {
            return 0xFFD700;
        }
        ComboTier tier = ComboTier.of(step);
        if (step < STRONG_FROM || tier == ComboTier.RAINBOW) {
            return -1;
        }
        return tier.rgb;
    }
}

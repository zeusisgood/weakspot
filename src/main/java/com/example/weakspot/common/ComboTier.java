package com.example.weakspot.common;

/** コンボの数の色の段階。1 以下は表示しない。 */
public enum ComboTier {
    NONE(0, 0xFFFFFF),
    WHITE(2, 0xFFFFFF),
    YELLOW(10, 0xFFFF55),
    ORANGE(25, 0xFFAA00),
    RED(50, 0xFF5555),
    /** 色はゆっくり変える（描画側で時間から決める）。rgb は使わない。 */
    RAINBOW(100, 0xFFFFFF);

    /** この段階になる最小のコンボ。 */
    public final int from;
    public final int rgb;

    ComboTier(int from, int rgb) {
        this.from = from;
        this.rgb = rgb;
    }

    public static ComboTier of(int combo) {
        ComboTier[] all = values();
        for (int i = all.length - 1; i > 0; i--) {
            if (combo >= all[i].from) {
                return all[i];
            }
        }
        return NONE;
    }
}

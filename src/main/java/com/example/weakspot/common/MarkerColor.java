package com.example.weakspot.common;

/** 他のプレイヤーの弱点マークの色（設定の "#RRGGBB"）。 */
public final class MarkerColor {

    private MarkerColor() {
    }

    /** "#RRGGBB" または "RRGGBB" を 0xRRGGBB にする。読めなければ fallback。 */
    public static int parse(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** rgb を白に amount（0〜1）だけ近づけた色の、各成分（0〜1）。 */
    public static float[] towardWhite(int rgb, float amount) {
        float[] c = {(rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F};
        for (int i = 0; i < 3; i++) {
            c[i] += (1 - c[i]) * amount;
        }
        return c;
    }
}

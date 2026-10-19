package io.github.zeusisgood.weakspot.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
* 投げる物と近接の溜めのゲージ。1 本分を越えた分は、赤い目盛りを足す（弓の過剰チャージと同じ形）。
* 「×n」の数字はゲージの下に出さない（照準の下の中央はコンボの表示と重なるため）。右に今の倍率を出す。
*/
final class ChargeGauge {

    private static final int MAX_MARKS = 5;
    private static final int MARK_HEIGHT = 2;
    private static final int MARK_GAP = 1;
    /** 弓の過剰チャージと同じ赤。 */
    private static final float[] MARK = {0xFF / 255F, 0x4D / 255F, 0x4D / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    private ChargeGauge() {
    }

    /** HudSpot.beginOverlay と endOverlay の間で呼ぶ。 */
    static void drawBars(Minecraft mc, double charge, double perBar, int rgb) {
        HudSpot.gauge(mc, charge / perBar, rgb, false);
        int marks = Math.min(MAX_MARKS, (int) Math.floor(charge / perBar) - 1);
        if (marks > 0) {
            drawMarks(mc, marks, MAX_MARKS);
        }
    }

    static void drawMarks(Minecraft mc, int lit, int count) {
        ScaledResolution res = new ScaledResolution(mc);
        double x0 = res.getScaledWidth() / 2.0 - HudSpot.GAUGE_WIDTH / 2.0;
        double top = res.getScaledHeight() / 2.0 + HudSpot.GAUGE_OFFSET + HudSpot.GAUGE_HEIGHT / 2.0 + MARK_GAP;
        double width = (HudSpot.GAUGE_WIDTH - MARK_GAP * (count - 1)) / (double) count;
        for (int i = 0; i < count; i++) {
            double left = x0 + i * (width + MARK_GAP);
            ScreenProjection.rect(left, top, left + width, top + MARK_HEIGHT, i < lit ? MARK : BACK);
        }
    }

    /** endOverlay のあとに呼ぶ。 */
    static void drawLabels(Minecraft mc, double charge, int rgb) {
        ScaledResolution res = new ScaledResolution(mc);
        int right = res.getScaledWidth() / 2 + HudSpot.GAUGE_WIDTH / 2 + 3;
        int y = res.getScaledHeight() / 2 + HudSpot.GAUGE_OFFSET - 4;
        mc.fontRenderer.drawStringWithShadow(String.format(Locale.ROOT, "×%.1f", 1 + charge), right, y, rgb);
    }
}

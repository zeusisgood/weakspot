package com.example.weakspot.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * 照準の下の、次の 1 回の強化の溜めのゲージ（1.7.0 の投げる物、1.8.0 の近接で共通）。ゲージ 1 本分 = perBar の溜め。
 * 越えた分は、1 本分ごとに赤い目盛りを 1 つ足す（弓の過剰チャージと同じ形。MAX_MARKS を越えたら「×n」の数字）。
 * ゲージの右に今の倍率（1 + 溜め）を出す。
 */
final class ChargeGauge {

    private static final int MAX_MARKS = 5;
    private static final int MARK_HEIGHT = 2;
    private static final int MARK_GAP = 1;
    /** 目盛りの赤 #FF4D4D（弓の過剰チャージと同じ）と、背景 #1E1E1E 半透明。 */
    private static final float[] MARK = {0xFF / 255F, 0x4D / 255F, 0x4D / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    private ChargeGauge() {
    }

    /** ゲージと目盛りを描く（HudSpot.beginOverlay と endOverlay の間）。数字が要るときは、その文字を返す（なければ null）。 */
    static String drawBars(Minecraft mc, double charge, double perBar, int rgb) {
        HudSpot.gauge(mc, charge / perBar, rgb, false);
        int marks = (int) Math.floor(charge / perBar) - 1;
        if (marks > MAX_MARKS) {
            return "×" + marks;
        }
        if (marks > 0) {
            ScaledResolution res = new ScaledResolution(mc);
            double x0 = res.getScaledWidth() / 2.0 - HudSpot.GAUGE_WIDTH / 2.0;
            double top = res.getScaledHeight() / 2.0 + HudSpot.GAUGE_OFFSET + HudSpot.GAUGE_HEIGHT / 2.0 + MARK_GAP;
            double width = (HudSpot.GAUGE_WIDTH - MARK_GAP * (MAX_MARKS - 1)) / (double) MAX_MARKS;
            for (int i = 0; i < MAX_MARKS; i++) {
                double left = x0 + i * (width + MARK_GAP);
                ScreenProjection.rect(left, top, left + width, top + MARK_HEIGHT, i < marks ? MARK : BACK);
            }
        }
        return null;
    }

    /** ゲージの右に今の倍率「×3.0」、目盛りが多すぎるときは、ゲージの下に「×n」を出す（endOverlay のあと）。 */
    static void drawLabels(Minecraft mc, double charge, int rgb, String extra) {
        ScaledResolution res = new ScaledResolution(mc);
        int right = res.getScaledWidth() / 2 + HudSpot.GAUGE_WIDTH / 2 + 3;
        int y = res.getScaledHeight() / 2 + HudSpot.GAUGE_OFFSET - 4;
        mc.fontRenderer.drawStringWithShadow(String.format(Locale.ROOT, "×%.1f", 1 + charge), right, y, rgb);
        if (extra != null) {
            int width = mc.fontRenderer.getStringWidth(extra);
            mc.fontRenderer.drawStringWithShadow(extra, res.getScaledWidth() / 2 - width / 2, y + 9, 0xFF4D4D);
        }
    }
}

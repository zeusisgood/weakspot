package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ComboDisplay;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.ComboMilestones;
import io.github.zeusisgood.weakspot.common.ComboTier;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.HitStreak;
import io.github.zeusisgood.weakspot.common.MachineComboBoost;
import io.github.zeusisgood.weakspot.common.ScheduledBoost;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.awt.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** 時間は ClientWeakSpotHandler.clientTick（一時停止中は止まる）。F1 で HUD を隠している間は描かない。 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class ComboHud {

    /** 照準の下の攻撃のクールダウン表示を避ける。 */
    private static final int CROSSHAIR_GAP = 16;
    private static final int BAR_WIDTH = 40;
    private static final double LOW_FRACTION = 0.25;
    private static final int LOW_RGB = 0xFF3333;
    /** コンボの数字に対する割合。 */
    private static final float KIND_LABEL_SCALE = 0.75F;
    /** GUI ピクセル。comboScale を掛ける。 */
    private static final float GAUGE_WIDTH = 40;

    private static final ComboMilestones MILESTONES = new ComboMilestones();

    /** 途切れたら 0。 */
    private static int combo;
    private static double lastHitTime;
    /** 途切れて消えていく途中のコンボ（0 なら何も残っていない）。 */
    private static int brokenCombo;
    private static double brokenTime;
    private static double stepTime = Double.NEGATIVE_INFINITY;
    private static int stepCombo;
    private static HitKind lastKind;
    /** 種類の表示の値（掛け数・機械の速さ）が上がった瞬間（光らせる）と、その段階の数。 */
    private static double factorStepTime = Double.NEGATIVE_INFINITY;
    private static int factorStepCombo;
    private static double shotsStepTime = Double.NEGATIVE_INFINITY;
    private static int shotsStepCombo;
    /** 「機械 ×n」をその下にそろえる。 */
    private static float lastCx;
    /** TOP_CENTER のときに避ける。 */
    private static int bossBottom;

    private ComboHud() {
    }

    /** time は tick（フレームの途中の値を含む）。 */
    static void onHit(HitKind kind, int newCombo, double time) {
        combo = newCombo;
        lastHitTime = time;
        brokenCombo = 0;
        if (kind != lastKind) {
            lastKind = kind;
            factorStepTime = Double.NEGATIVE_INFINITY;
        }
        // 上限で止まっていれば光らない
        if (labelValue(kind, newCombo) > labelValue(kind, newCombo - 1) + 1e-9) {
            factorStepTime = time;
            factorStepCombo = newCombo;
        }
        if (dispenseCount(newCombo) > dispenseCount(newCombo - 1)) {
            shotsStepTime = time;
            shotsStepCombo = newCombo;
        }
        if (MILESTONES.reached(newCombo) && WeakSpotConfig.client.combo.comboDisplayEnabled
                && WeakSpotConfig.client.combo.comboMilestoneEffects) {
            stepTime = time;
            stepCombo = newCombo;
            ComboEffects.playStep(newCombo);
        }
    }

    /** 数字だけを書き換え、弾み・光・音は出さない。0 なら「MAX n」を残さずに消す。 */
    static void correct(int newCombo) {
        combo = Math.max(0, newCombo);
    }

    static void onBreak(int broken, long tick) {
        combo = 0;
        if (broken >= ComboDisplay.MIN_SHOWN) {
            brokenCombo = broken;
            brokenTime = tick;
        }
    }

    /** その場で消す。 */
    static void clear() {
        combo = 0;
        brokenCombo = 0;
        stepTime = Double.NEGATIVE_INFINITY;
        stepCombo = 0;
        factorStepTime = Double.NEGATIVE_INFINITY;
        lastKind = null;
        shotsStepTime = Double.NEGATIVE_INFINITY;
        ComboEffects.clear();
        MILESTONES.reset();
    }

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiEvent.Pre event) {
        bossBottom = 0;
    }

    @SubscribeEvent
    public static void onBossInfo(CustomizeGuiOverlayEvent.BossEventProgress event) {
        // 名前はバーの 9 上、バーの高さは 5
        bossBottom = Math.max(bossBottom, event.getY() + 5);
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!WeakSpotConfig.client.combo.comboDisplayEnabled || mc.options.hideGui || mc.player == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        double now = ClientWeakSpotHandler.clientTick + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        ComboEffects.drawTitle(g, mc, now);
        if (combo >= ComboDisplay.MIN_SHOWN) {
            boolean big = stepCombo >= ComboMilestones.STRONG_FROM && lastHitTime == stepTime;
            double bounce = ComboDisplay.bounceScale(now - lastHitTime,
                    big ? ComboDisplay.BIG_BOUNCE_PEAK : ComboDisplay.BOUNCE_PEAK);
            float bottom = draw(g, mc, I18n.get("weakspot.combo.hit", combo), colorOf(combo), 1,
                    bounce, HitStreak.remainingFraction(now - lastHitTime),
                    ComboDisplay.glowAlpha(now - stepTime), glowRgb(stepCombo), stepCombo >= ComboMilestones.STRONG_FROM, now);
            drawKindLabel(g, mc, bottom, now);
        } else if (combo == 1 && lastKind == HitKind.MACHINE && ClientWeakSpotHandler.machineSpotActive()) {
            // 1 ヒット目はコンボの数字が出ないので、数字の位置に機械の表示だけを出す
            Font font = mc.font;
            float scale = (float) WeakSpotConfig.client.combo.comboScale;
            float[] center = center(mc, 0, font.lineHeight * scale);
            lastCx = center[0];
            drawKindLabel(g, mc, center[1] - font.lineHeight * scale / 2, now);
        } else if (brokenCombo > 0) {
            double alpha = ComboDisplay.fadeAlpha(brokenCombo, now - brokenTime);
            if (alpha <= 0) {
                brokenCombo = 0;
                return;
            }
            String text = ComboDisplay.showsMax(brokenCombo)
                    ? I18n.get("weakspot.combo.max", brokenCombo)
                    : I18n.get("weakspot.combo.hit", brokenCombo);
            draw(g, mc, text, colorOf(brokenCombo), alpha, 1, -1, 0, 0, false, now);
        }
    }

    private static int glowRgb(int step) {
        int rgb = ComboMilestones.glowRgb(step);
        return rgb >= 0 ? rgb : colorOf(step);
    }

    private static void drawKindLabel(GuiGraphics g, Minecraft mc, float top, double now) {
        HitKind kind = lastKind;
        if (kind == null || !spotShown(kind)) {
            return;
        }
        float bottom = drawKindText(g, mc, top, kind, now);
        if (WeakSpotConfig.client.combo.comboFactorGaugeEnabled && (kind.usesComboFactor() || kind == HitKind.MACHINE)) {
            drawFactorGauge(g, bottom);
        }
    }

    /** top は種類の表示の下端（表示がなければコンボの表示の下端）。 */
    private static void drawFactorGauge(GuiGraphics g, float top) {
        double progress = ComboFactor.progressToNext(combo);
        float scale = (float) WeakSpotConfig.client.combo.comboScale;
        float width = GAUGE_WIDTH * scale;
        float height = Math.max(1, Math.round(2 * scale));
        float y = top + 2 * scale;
        float left = lastCx - width / 2;
        fillRect(g, left, y, left + width, y + height, 0x000000, 0.4);
        fillRect(g, left, y, left + (float) (width * progress), y + height, colorOf(ComboFactor.nextStep(combo)), 0.9);
    }

    /** 描いた下端を返す（描かなければ top）。 */
    private static float drawKindText(GuiGraphics g, Minecraft mc, float top, HitKind kind, double now) {
        int rgb = MarkerLook.color(kind);
        boolean crafter = kind == HitKind.MACHINE && ClientWeakSpotHandler.crafterSpotActive();
        if (crafter || kind == HitKind.MACHINE && ClientWeakSpotHandler.dispenserSpotActive()) {
            // 上限の設定も考える
            return drawKindLabel(g, mc, top,
                    I18n.get(crafter ? "weakspot.combo.crafts" : "weakspot.combo.shots", dispenseCount(combo)), rgb,
                    ComboDisplay.glowAlpha(now - shotsStepTime), glowRgb(shotsStepCombo));
        } else if (kind == HitKind.MACHINE) {
            // サーバーと同じ計算
            return drawKindLabel(g, mc, top, I18n.get("weakspot.combo.machine",
                    MachineComboBoost.speedLabel(machineSpeed(combo))), rgb,
                    ComboDisplay.glowAlpha(now - factorStepTime), glowRgb(factorStepCombo));
        } else if (kind.usesComboFactor()) {
            double factor = labelValue(kind, combo);
            if (factor > 1) {
                return drawKindLabel(g, mc, top, I18n.get("weakspot.combo.kind",
                        I18n.get("weakspot.kind." + kind.key()), String.valueOf(factor)), rgb,
                        ComboDisplay.glowAlpha(now - factorStepTime), glowRgb(factorStepCombo));
            }
        }
        return top;
    }

    /** 採掘・機械・収穫は、このフレームで照準が合っていることも見る。 */
    private static boolean spotShown(HitKind kind) {
        switch (kind) {
            case MINING:
            case MACHINE:
                return ClientWeakSpotHandler.blockSpotActive(kind);
            case HARVEST:
                // 収穫すると植え直して、同じ所がすぐ成長の弱点に変わるので、それも含める
                return ClientWeakSpotHandler.blockSpotActive(HitKind.HARVEST)
                        || ClientWeakSpotHandler.blockSpotActive(HitKind.GROWTH);
            default:
                return AimSpots.isShown(kind);
        }
    }

    private static double labelValue(HitKind kind, int combo) {
        if (kind == HitKind.MACHINE) {
            return machineSpeed(combo);
        }
        if (kind == HitKind.MINING) {
            return MiningBoost.comboFactor(Math.max(0, combo));
        }
        return kind.usesComboFactor() ? ComboFactor.factor(Math.max(0, combo)) : 1;
    }

    /** サーバーから届いた倍率と上限で、サーバーと同じ計算をする。 */
    private static double machineSpeed(int combo) {
        SyncedSettings settings = ClientSettings.get();
        return MachineComboBoost.multiplier(settings.machineBoostMultiplier, settings.machineBoostMaxMultiplier,
                Math.max(1, combo));
    }

    private static int dispenseCount(int combo) {
        return ScheduledBoost.dispenseCount(machineSpeed(combo), ClientSettings.get().machineBoostMultiplier);
    }

    /** top はコンボの表示の下端。 */
    private static float drawKindLabel(GuiGraphics g, Minecraft mc, float top, String text, int baseRgb, double glow,
                                       int glowRgb) {
        Font font = mc.font;
        float scale = (float) WeakSpotConfig.client.combo.comboScale * KIND_LABEL_SCALE;
        float width = font.width(text) * scale;
        float height = font.lineHeight * scale;
        float cy = top + 2 * scale + height / 2;
        if (glow > 0) {
            float pad = (float) (1 + 4 * (1 - glow)) * scale;
            fillRect(g, lastCx - width / 2 - pad, cy - height / 2 - pad, lastCx + width / 2 + pad, cy + height / 2 + pad,
                    glowRgb, 0.5 * glow);
        }
        g.pose().pushPose();
        g.pose().translate(lastCx, cy, 0);
        g.pose().scale(scale, scale, 1);
        int rgb = glow > 0.5 ? glowRgb : baseRgb;
        g.drawString(font, text, -font.width(text) / 2F, -font.lineHeight / 2F + 1, 0xFF000000 | rgb, true);
        g.pose().popPose();
        return cy + height / 2;
    }

    private static float[] center(Minecraft mc, float textWidth, float textHeight) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        switch (WeakSpotConfig.client.combo.comboPosition) {
            case RIGHT_OF_CROSSHAIR:
                return new float[] {width / 2F + CROSSHAIR_GAP + textWidth / 2, height / 2F};
            case TOP_CENTER:
                return new float[] {width / 2F, Math.max(4, bossBottom + 4) + textHeight / 2};
            case BELOW_CROSSHAIR:
            default:
                return new float[] {width / 2F, height / 2F + CROSSHAIR_GAP + textHeight / 2};
        }
    }

    /**
    * @param barFraction 0〜1。負なら描かない
    * @param glow 0 なら描かない
    * @return 描いたものの下端（「機械 ×n」をその下に出す）
    */
    private static float draw(GuiGraphics g, Minecraft mc, String text, int rgb, double alpha, double bounce,
                              double barFraction, double glow, int glowRgb, boolean strongGlow, double now) {
        Font font = mc.font;
        float scale = (float) WeakSpotConfig.client.combo.comboScale;
        String shown = ChatFormatting.BOLD + text;
        float textWidth = font.width(shown) * scale;
        float textHeight = font.lineHeight * scale;
        float barHeight = Math.max(1, Math.round(2 * scale));
        float barGap = 2 * scale;

        float[] center = center(mc, textWidth, textHeight);
        float cx = center[0];
        float cy = center[1];
        lastCx = cx;
        float bottom = cy + textHeight / 2;

        if (glow > 0) {
            float pad = (float) (strongGlow ? 3 + 12 * (1 - glow) : 2 + 6 * (1 - glow)) * scale;
            fillRect(g, cx - textWidth / 2 - pad, cy - textHeight / 2 - pad, cx + textWidth / 2 + pad,
                    cy + textHeight / 2 + pad, glowRgb, (strongGlow ? 0.8 : 0.5) * glow * alpha);
        }
        if (barFraction >= 0) {
            float width = BAR_WIDTH * scale;
            float top = cy + textHeight / 2 + barGap;
            float left = cx - width / 2;
            fillRect(g, left, top, left + width, top + barHeight, 0x000000, 0.4 * alpha);
            boolean low = barFraction < LOW_FRACTION;
            double barAlpha = low && (long) (now / 2) % 2 == 0 ? 0.5 : 1;
            fillRect(g, left, top, left + (float) (width * barFraction), top + barHeight, low ? LOW_RGB : rgb,
                    barAlpha * alpha);
            bottom = top + barHeight;
        }

        int a = (int) Math.round(alpha * 255);
        // Font は濃さがほぼ 0 の色を不透明として扱うので、薄すぎるときは描かない
        if (a >= 8) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            float k = (float) (scale * bounce);
            g.pose().scale(k, k, 1);
            g.drawString(font, shown, -font.width(shown) / 2F, -font.lineHeight / 2F + 1, a << 24 | rgb, true);
            g.pose().popPose();
        }
        return bottom;
    }

    /** 頭の上のコンボ（OtherCombos）も使う。 */
    static int colorOf(int value) {
        ComboTier tier = ComboTier.of(value);
        if (tier != ComboTier.RAINBOW) {
            return tier.rgb;
        }
        long period = ComboDisplay.rainbowPeriodMs(value);
        float hue = (Util.getMillis() % period) / (float) period;
        return Color.HSBtoRGB(hue, 0.55F, 1.0F) & 0xFFFFFF;
    }

    private static void fillRect(GuiGraphics g, float x1, float y1, float x2, float y2, int rgb, double alpha) {
        if (x2 <= x1 || alpha <= 0) {
            return;
        }
        float r = (rgb >> 16 & 0xFF) / 255F;
        float gr = (rgb >> 8 & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        float a = (float) alpha;
        HudSpot.beginOverlay();
        Draw.setPose(g.pose().last().pose());
        BufferBuilder buffer = Draw.begin(Draw.QUADS);
        Draw.vertex(buffer, x1, y2, 0, r, gr, b, a);
        Draw.vertex(buffer, x2, y2, 0, r, gr, b, a);
        Draw.vertex(buffer, x2, y1, 0, r, gr, b, a);
        Draw.vertex(buffer, x1, y1, 0, r, gr, b, a);
        Draw.end(buffer);
        Draw.setPose(null);
        HudSpot.endOverlay();
    }
}

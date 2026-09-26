package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.ComboDisplay;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.common.ComboMilestones;
import com.example.weakspot.common.ComboTier;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.HitStreak;
import com.example.weakspot.common.MachineComboBoost;
import com.example.weakspot.common.ScheduledBoost;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/**
 * 自分の連続ヒット（コンボ）の画面表示。数は OwnHits.STREAK（ヒット音の音階と同じ数）。
 * 2 以上で「12 HIT」と出し、ヒットのたびに弾ませる。下のバーは途切れるまでの残り時間。
 * 途切れたら薄くして消す（5 以上なら「MAX 23」を少し残す）。10、25、50、100、250、500、1000（以降 1000 ごと）で
 * 光を出す（音・花火・タイトルは ComboEffects。1.8.9 で分けた）。
 * 直前にヒットした種類の弱点が出ている間は、その下に種類の表示を出す（1.8.7）: コンボの掛け数を使う種類は「走り ×1.25」
 * （掛け数が 1 より大きいとき）、機械は今の速さ「機械 4倍速」、ディスペンサー・ドロッパーは「発射 ×2」。色はその種類の弱点の色。
 * 時間は ClientWeakSpotHandler.clientTick（一時停止中は止まる）で数える。F1 で HUD を隠している間は描かない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class ComboHud {

    /** 照準からの距離（照準の下の攻撃のクールダウン表示を避ける）。 */
    private static final int CROSSHAIR_GAP = 16;
    private static final int BAR_WIDTH = 40;
    /** 残りがこの割合を切ったら、バーを点滅させる。 */
    private static final double LOW_FRACTION = 0.25;
    private static final int LOW_RGB = 0xFF3333;
    /** 種類の表示（「走り ×1.25」「機械 4倍速」）の文字の大きさ（コンボの数字に対する割合）。 */
    private static final float KIND_LABEL_SCALE = 0.75F;

    private static final ComboMilestones MILESTONES = new ComboMilestones();

    /** 表示中のコンボ（途切れたら 0）。 */
    private static int combo;
    private static double lastHitTime;
    /** 途切れて消えていく途中のコンボ（0 なら何も残っていない）。 */
    private static int brokenCombo;
    private static double brokenTime;
    private static double stepTime = Double.NEGATIVE_INFINITY;
    /** 最後に達した段階の数（光の色と弾みの大きさを決める）。 */
    private static int stepCombo;
    /** 直前にヒットした種類（種類の表示に使う）。 */
    private static HitKind lastKind;
    /** 種類の表示の値（掛け数・機械の速さ）が上がった瞬間（光らせる）と、その段階の数。 */
    private static double factorStepTime = Double.NEGATIVE_INFINITY;
    private static int factorStepCombo;
    /** 「発射 ×n」の回数が上がった瞬間（光らせる）と、その段階の数。 */
    private static double shotsStepTime = Double.NEGATIVE_INFINITY;
    private static int shotsStepCombo;
    /** 最後に描いたコンボの数字の中心の x（「機械 ×n」をその下にそろえる）。 */
    private static float lastCx;
    /** このフレームのボスバーの下端（TOP_CENTER のときに避ける）。 */
    private static int bossBottom;

    private ComboHud() {
    }

    /** 自分のヒット。time はヒットした瞬間（tick。フレームの途中の値を含む）。 */
    static void onHit(HitKind kind, int newCombo, double time) {
        combo = newCombo;
        lastHitTime = time;
        // 新しいコンボが始まったら、残していた数は消す
        brokenCombo = 0;
        if (kind != lastKind) {
            lastKind = kind;
            factorStepTime = Double.NEGATIVE_INFINITY;
        }
        // 掛け数・速さ・発射の回数が上がった瞬間は光らせる（上限で止まっていれば光らない）
        if (labelValue(kind, newCombo) > labelValue(kind, newCombo - 1) + 1e-9) {
            factorStepTime = time;
            factorStepCombo = newCombo;
        }
        if (dispenseCount(newCombo) > dispenseCount(newCombo - 1)) {
            shotsStepTime = time;
            shotsStepCombo = newCombo;
        }
        if (MILESTONES.reached(newCombo) && WeakSpotConfig.comboDisplayEnabled
                && WeakSpotConfig.comboMilestoneEffects) {
            stepTime = time;
            stepCombo = newCombo;
            ComboEffects.playStep(newCombo);
        }
    }

    /** 40 tick ヒットがなく途切れた。 */
    static void onBreak(int broken, long tick) {
        combo = 0;
        if (broken >= ComboDisplay.MIN_SHOWN) {
            brokenCombo = broken;
            brokenTime = tick;
        }
    }

    /** ワールドを出た、死亡した、ディメンションを移動した（その場で消す）。 */
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
    public static void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            bossBottom = 0;
        }
    }

    @SubscribeEvent
    public static void onBossInfo(RenderGameOverlayEvent.BossInfo event) {
        // 名前はバーの 9 上、バーの高さは 5
        bossBottom = Math.max(bossBottom, event.getY() + 5);
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!WeakSpotConfig.comboDisplayEnabled || mc.gameSettings.hideGUI || mc.player == null) {
            return;
        }
        double now = ClientWeakSpotHandler.clientTick + event.getPartialTicks();
        ComboEffects.drawTitle(mc, event.getResolution(), now);
        if (combo >= ComboDisplay.MIN_SHOWN) {
            boolean big = stepCombo >= 250 && lastHitTime == stepTime;
            double bounce = ComboDisplay.bounceScale(now - lastHitTime,
                    big ? ComboDisplay.BIG_BOUNCE_PEAK : ComboDisplay.BOUNCE_PEAK);
            float bottom = draw(mc, event.getResolution(), I18n.format("weakspot.combo.hit", combo), colorOf(combo), 1,
                    bounce, HitStreak.remainingFraction(now - lastHitTime),
                    ComboDisplay.glowAlpha(now - stepTime), glowRgb(stepCombo), stepCombo >= 250, now);
            drawKindLabel(mc, bottom, now);
        } else if (combo == 1 && lastKind == HitKind.MACHINE && ClientWeakSpotHandler.machineSpotActive()) {
            // 1 ヒット目はコンボの数字が出ないので、数字の位置に機械の表示だけを出す（1.6.0）
            FontRenderer font = mc.fontRenderer;
            float scale = (float) WeakSpotConfig.comboScale;
            float[] center = center(event.getResolution(), 0, font.FONT_HEIGHT * scale);
            lastCx = center[0];
            drawKindLabel(mc, center[1] - font.FONT_HEIGHT * scale / 2, now);
        } else if (brokenCombo > 0) {
            double alpha = ComboDisplay.fadeAlpha(brokenCombo, now - brokenTime);
            if (alpha <= 0) {
                brokenCombo = 0;
                return;
            }
            String text = ComboDisplay.showsMax(brokenCombo)
                    ? I18n.format("weakspot.combo.max", brokenCombo)
                    : I18n.format("weakspot.combo.hit", brokenCombo);
            draw(mc, event.getResolution(), text, colorOf(brokenCombo), alpha, 1, -1, 0, 0, false, now);
        }
    }

    /** 段階の光の色（250 以上は段階の色、それより下はその数の色）。 */
    private static int glowRgb(int step) {
        int rgb = ComboMilestones.glowRgb(step);
        return rgb >= 0 ? rgb : colorOf(step);
    }

    /**
     * 直前にヒットした種類の弱点が今出ていれば、その下に種類の表示を出す（1.8.7。1.8.6 までは機械だけ）。
     * ディスペンサー・ドロッパーは「発射 ×2」、機械は「機械 4倍速」、コンボの掛け数を使う種類は、掛け数が 1 より大きいとき
     * 「走り ×1.25」。色はその種類の自分の弱点の色（段階が上がった瞬間は、その段階の色で光る）。
     */
    private static void drawKindLabel(Minecraft mc, float top, double now) {
        HitKind kind = lastKind;
        if (kind == null || !spotShown(kind)) {
            return;
        }
        int rgb = MarkerLook.color(kind);
        if (kind == HitKind.MACHINE && ClientWeakSpotHandler.dispenserSpotActive()) {
            // ディスペンサー・ドロッパーは、1 回の信号での発射の回数を出す（1.5.2。1.6.0 から上限の設定も考える）
            drawKindLabel(mc, top, I18n.format("weakspot.combo.shots", dispenseCount(combo)), rgb,
                    ComboDisplay.glowAlpha(now - shotsStepTime), glowRgb(shotsStepCombo));
        } else if (kind == HitKind.MACHINE) {
            // その機械の今の速さ（サーバーと同じ計算。1.6.0）
            drawKindLabel(mc, top, I18n.format("weakspot.combo.machine",
                    MachineComboBoost.speedLabel(machineSpeed(combo))), rgb,
                    ComboDisplay.glowAlpha(now - factorStepTime), glowRgb(factorStepCombo));
        } else if (kind.usesComboFactor()) {
            double factor = labelValue(kind, combo);
            if (factor > 1) {
                drawKindLabel(mc, top, I18n.format("weakspot.combo.kind", I18n.format("weakspot.kind." + kind.key()),
                        MachineComboBoost.label(factor)), rgb,
                        ComboDisplay.glowAlpha(now - factorStepTime), glowRgb(factorStepCombo));
            }
        }
    }

    /** その種類の自分の弱点が、今出ているか（採掘・機械・収穫はこのフレームで照準が合っている）。 */
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

    /** 種類の表示の値: 機械は速さ、ほかはコンボの掛け数（採掘は、サーバーが 1.8.7 より前なら 1）。 */
    private static double labelValue(HitKind kind, int combo) {
        if (kind == HitKind.MACHINE) {
            return machineSpeed(combo);
        }
        if (kind == HitKind.MINING) {
            return MiningBoost.comboFactor(Math.max(0, combo));
        }
        return kind.usesComboFactor() ? ComboFactor.factor(Math.max(0, combo)) : 1;
    }

    /** コンボ combo での機械の速さ（サーバーから届いた倍率と上限で、サーバーと同じ計算）。 */
    private static double machineSpeed(int combo) {
        SyncedSettings settings = ClientSettings.get();
        return MachineComboBoost.multiplier(settings.machineBoostMultiplier, settings.machineBoostMaxMultiplier,
                Math.max(1, combo));
    }

    /** コンボ combo でのディスペンサー・ドロッパーの 1 回の信号での発射の回数。 */
    private static int dispenseCount(int combo) {
        return ScheduledBoost.dispenseCount(machineSpeed(combo), ClientSettings.get().machineBoostMultiplier);
    }

    /** 「機械 4倍速」「走り ×1.25」。top はコンボの表示の下端、cx はその中心。 */
    private static void drawKindLabel(Minecraft mc, float top, String text, int baseRgb, double glow, int glowRgb) {
        FontRenderer font = mc.fontRenderer;
        float scale = (float) WeakSpotConfig.comboScale * KIND_LABEL_SCALE;
        float width = font.getStringWidth(text) * scale;
        float height = font.FONT_HEIGHT * scale;
        float cy = top + 2 * scale + height / 2;
        if (glow > 0) {
            float pad = (float) (1 + 4 * (1 - glow)) * scale;
            fillRect(lastCx - width / 2 - pad, cy - height / 2 - pad, lastCx + width / 2 + pad, cy + height / 2 + pad,
                    glowRgb, 0.5 * glow);
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(lastCx, cy, 0);
        GlStateManager.scale(scale, scale, 1);
        int rgb = glow > 0.5 ? glowRgb : baseRgb;
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2F, -font.FONT_HEIGHT / 2F + 1, 0xFF000000 | rgb);
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
    }

    /** コンボの数字の中心 {x, y}（設定 comboPosition による）。 */
    private static float[] center(ScaledResolution res, float textWidth, float textHeight) {
        switch (WeakSpotConfig.comboPosition) {
            case RIGHT_OF_CROSSHAIR:
                return new float[] {res.getScaledWidth() / 2F + CROSSHAIR_GAP + textWidth / 2, res.getScaledHeight() / 2F};
            case TOP_CENTER:
                return new float[] {res.getScaledWidth() / 2F, Math.max(4, bossBottom + 4) + textHeight / 2};
            case BELOW_CROSSHAIR:
            default:
                return new float[] {res.getScaledWidth() / 2F, res.getScaledHeight() / 2F + CROSSHAIR_GAP + textHeight / 2};
        }
    }

    /**
     * @param barFraction 残り時間のバー（0〜1）。負なら描かない
     * @param glow 段階の演出の光の濃さ（0 なら描かない）
     * @param strongGlow 250 以上の段階の強い光
     * @return 描いたものの下端（「機械 ×n」をその下に出す）
     */
    private static float draw(Minecraft mc, ScaledResolution res, String text, int rgb, double alpha, double bounce,
                              double barFraction, double glow, int glowRgb, boolean strongGlow, double now) {
        FontRenderer font = mc.fontRenderer;
        float scale = (float) WeakSpotConfig.comboScale;
        String shown = TextFormatting.BOLD + text;
        float textWidth = font.getStringWidth(shown) * scale;
        float textHeight = font.FONT_HEIGHT * scale;
        float barHeight = Math.max(1, Math.round(2 * scale));
        float barGap = 2 * scale;

        // 数字の中心
        float[] center = center(res, textWidth, textHeight);
        float cx = center[0];
        float cy = center[1];
        lastCx = cx;
        float bottom = cy + textHeight / 2;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        if (glow > 0) {
            float pad = (float) (strongGlow ? 3 + 12 * (1 - glow) : 2 + 6 * (1 - glow)) * scale;
            fillRect(cx - textWidth / 2 - pad, cy - textHeight / 2 - pad, cx + textWidth / 2 + pad,
                    cy + textHeight / 2 + pad, glowRgb, (strongGlow ? 0.8 : 0.5) * glow * alpha);
        }
        if (barFraction >= 0) {
            float width = BAR_WIDTH * scale;
            float top = cy + textHeight / 2 + barGap;
            float left = cx - width / 2;
            fillRect(left, top, left + width, top + barHeight, 0x000000, 0.4 * alpha);
            boolean low = barFraction < LOW_FRACTION;
            // 途切れそうなときは赤く点滅させる
            double barAlpha = low && (long) (now / 2) % 2 == 0 ? 0.5 : 1;
            fillRect(left, top, left + (float) (width * barFraction), top + barHeight, low ? LOW_RGB : rgb,
                    barAlpha * alpha);
            bottom = top + barHeight;
        }

        int a = (int) Math.round(alpha * 255);
        // FontRenderer は濃さがほぼ 0 の色を不透明として扱うので、薄すぎるときは描かない
        if (a >= 8) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0);
            float k = (float) (scale * bounce);
            GlStateManager.scale(k, k, 1);
            font.drawStringWithShadow(shown, -font.getStringWidth(shown) / 2F, -font.FONT_HEIGHT / 2F + 1,
                    a << 24 | rgb);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1, 1, 1, 1);
        return bottom;
    }

    /** コンボの数の色（段階の色。100 以上は虹色）。頭の上のコンボ（OtherCombos）も使う。 */
    static int colorOf(int value) {
        ComboTier tier = ComboTier.of(value);
        if (tier != ComboTier.RAINBOW) {
            return tier.rgb;
        }
        long period = ComboDisplay.rainbowPeriodMs(value);
        float hue = (Minecraft.getSystemTime() % period) / (float) period;
        return Color.HSBtoRGB(hue, 0.55F, 1.0F) & 0xFFFFFF;
    }

    private static void fillRect(float x1, float y1, float x2, float y2, int rgb, double alpha) {
        if (x2 <= x1 || alpha <= 0) {
            return;
        }
        float r = (rgb >> 16 & 0xFF) / 255F;
        float g = (rgb >> 8 & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        float a = (float) alpha;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }
}

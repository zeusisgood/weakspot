package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.ComboDisplay;
import com.example.weakspot.common.ComboMilestones;
import com.example.weakspot.common.ComboTier;
import com.example.weakspot.common.HitPitch;
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
 * 自分の連続ヒット（コンボ）の画面表示。数は ClientWeakSpotHandler.STREAK（ヒット音の音階と同じ数）。
 * 2 以上で「12 HIT」と出し、ヒットのたびに弾ませる。下のバーは途切れるまでの残り時間。
 * 途切れたら薄くして消す（5 以上なら「MAX 23」を少し残す）。10、25、50、100、250、500、1000（以降 1000 ごと）で
 * 強調音と光を出し、上の段階ほど派手にする（100 は和音、250 から駆け上がり、500 から花火、1000 からタイトル）。
 * 機械の弱点に照準が合っている間は、コンボの掛け数（MachineComboBoost）を「機械 ×2.5」と下に出す。
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
    /** 段階の演出の強調音を、ヒット音から少し遅らせる（tick）。 */
    private static final int ACCENT_DELAY_TICKS = 2;
    /** 100 の和音（ド・ミ・ソ・上のド。連続ヒット数で表した音階の位置）。 */
    private static final int[] CHORD = {1, 3, 5, HitPitch.SCALE_LENGTH};
    /** 250 の駆け上がりの音の数（ドからソまで）。 */
    private static final int SHORT_RUN = 5;
    /** 「機械 ×n」の文字の大きさ（コンボの数字に対する割合）。 */
    private static final float MACHINE_LABEL_SCALE = 0.75F;
    private static final int MACHINE_LABEL_RGB = 0xFFFFFF;
    /** 1000 のタイトル（フェードイン、表示、フェードアウトの tick）。バニラのタイトルと同じ長さ。 */
    private static final int TITLE_IN = 5;
    private static final int TITLE_STAY = 50;
    private static final int TITLE_OUT = 20;
    /** 節目のタイトルがこの tick 以内に出ていたら、1000 のタイトルは出さない（節目を優先する）。 */
    private static final int TITLE_GUARD_TICKS = TITLE_IN + TITLE_STAY + TITLE_OUT;
    private static final float TITLE_SCALE = 3;

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
    /** 「機械 n倍速」の速さが上がった瞬間（光らせる）と、その段階の数。 */
    private static double factorStepTime = Double.NEGATIVE_INFINITY;
    private static int factorStepCombo;
    /** 「発射 ×n」の回数が上がった瞬間（光らせる）と、その段階の数。 */
    private static double shotsStepTime = Double.NEGATIVE_INFINITY;
    private static int shotsStepCombo;
    /** 1000 のタイトルを出し始めた clientTick と、その数（0 なら出していない）。 */
    private static long titleTick;
    private static int titleCombo;
    /** 最後に描いたコンボの数字の中心の x（「機械 ×n」をその下にそろえる）。 */
    private static float lastCx;
    /** このフレームのボスバーの下端（TOP_CENTER のときに避ける）。 */
    private static int bossBottom;

    private ComboHud() {
    }

    /** 自分のヒット。time はヒットした瞬間（tick。フレームの途中の値を含む）。 */
    static void onHit(int newCombo, double time) {
        combo = newCombo;
        lastHitTime = time;
        // 新しいコンボが始まったら、残していた数は消す
        brokenCombo = 0;
        // 速さ・発射の回数が上がった瞬間は光らせる（上限で止まっていれば光らない）
        if (machineSpeed(newCombo) > machineSpeed(newCombo - 1) + 1e-9) {
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
            playStep(newCombo);
        }
    }

    /** 段階の演出。上の段階ほど足していく。 */
    private static void playStep(int step) {
        if (step >= ComboMilestones.GRAND_STEP) {
            for (int i = 0; i < HitPitch.TWO_OCTAVE_LENGTH; i++) {
                float pitch = HitPitch.twoOctave(i);
                HitSounds.schedule(ACCENT_DELAY_TICKS + i, () -> HitSounds.playOwnPitch(pitch));
            }
            playChord(ACCENT_DELAY_TICKS + HitPitch.TWO_OCTAVE_LENGTH + 1);
            HitSounds.schedule(ACCENT_DELAY_TICKS, () -> {
                MilestoneEffects.comboFireworks(3, ComboMilestones.glowRgb(step));
                long now = ClientWeakSpotHandler.clientTick;
                // 累計の節目のタイトルが出ていたら、そちらを優先する
                if (now - MilestoneEffects.lastShownTick > TITLE_GUARD_TICKS) {
                    titleTick = now;
                    titleCombo = step;
                }
            });
        } else if (step >= 500) {
            HitSounds.playScale(HitSounds::playOwn, 1, ACCENT_DELAY_TICKS);
            HitSounds.schedule(ACCENT_DELAY_TICKS,
                    () -> MilestoneEffects.comboFireworks(1, ComboMilestones.glowRgb(step)));
        } else if (step >= 250) {
            for (int i = 0; i < SHORT_RUN; i++) {
                int note = i + 1;
                HitSounds.schedule(ACCENT_DELAY_TICKS + i, () -> HitSounds.playOwn(note));
            }
        } else if (step >= 100) {
            playChord(ACCENT_DELAY_TICKS);
        } else {
            HitSounds.schedule(ACCENT_DELAY_TICKS, () -> HitSounds.playOwn(HitPitch.SCALE_LENGTH));
        }
    }

    private static void playChord(int delay) {
        HitSounds.schedule(delay, () -> {
            for (int note : CHORD) {
                HitSounds.playOwn(note);
            }
        });
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
        shotsStepTime = Double.NEGATIVE_INFINITY;
        titleCombo = 0;
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
        drawTitle(mc, event.getResolution(), now);
        if (combo >= ComboDisplay.MIN_SHOWN) {
            boolean big = stepCombo >= 250 && lastHitTime == stepTime;
            double bounce = ComboDisplay.bounceScale(now - lastHitTime,
                    big ? ComboDisplay.BIG_BOUNCE_PEAK : ComboDisplay.BOUNCE_PEAK);
            float bottom = draw(mc, event.getResolution(), I18n.format("weakspot.combo.hit", combo), colorOf(combo), 1,
                    bounce, HitStreak.remainingFraction(now - lastHitTime),
                    ComboDisplay.glowAlpha(now - stepTime), glowRgb(stepCombo), stepCombo >= 250, now);
            drawMachineLabel(mc, bottom, now);
        } else if (combo == 1 && ClientWeakSpotHandler.machineSpotActive()) {
            // 1 ヒット目はコンボの数字が出ないので、数字の位置に機械の表示だけを出す（1.6.0）
            FontRenderer font = mc.fontRenderer;
            float scale = (float) WeakSpotConfig.comboScale;
            float[] center = center(event.getResolution(), 0, font.FONT_HEIGHT * scale);
            lastCx = center[0];
            drawMachineLabel(mc, center[1] - font.FONT_HEIGHT * scale / 2, now);
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

    /** 機械の弱点に照準が合っていれば、その下に「機械 4倍速」（ディスペンサー・ドロッパーは「発射 ×2」）を出す。 */
    private static void drawMachineLabel(Minecraft mc, float top, double now) {
        if (ClientWeakSpotHandler.dispenserSpotActive()) {
            // ディスペンサー・ドロッパーは、1 回の信号での発射の回数を出す（1.5.2。1.6.0 から上限の設定も考える）
            drawMachineLabel(mc, top, I18n.format("weakspot.combo.shots", dispenseCount(combo)),
                    ComboDisplay.glowAlpha(now - shotsStepTime), glowRgb(shotsStepCombo));
        } else if (ClientWeakSpotHandler.machineSpotActive()) {
            // その機械の今の速さ（サーバーと同じ計算。1.6.0）
            drawMachineLabel(mc, top, I18n.format("weakspot.combo.machine",
                    MachineComboBoost.speedLabel(machineSpeed(combo))),
                    ComboDisplay.glowAlpha(now - factorStepTime), glowRgb(factorStepCombo));
        }
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

    /** 「機械 4倍速」。top はコンボの表示の下端、cx はその中心。 */
    private static void drawMachineLabel(Minecraft mc, float top, String text, double glow, int glowRgb) {
        FontRenderer font = mc.fontRenderer;
        float scale = (float) WeakSpotConfig.comboScale * MACHINE_LABEL_SCALE;
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
        int rgb = glow > 0.5 ? glowRgb : MACHINE_LABEL_RGB;
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

    /** 1000（以降 1000 ごと）のタイトル。画面の中央の少し上に、金色で大きく出す。 */
    private static void drawTitle(Minecraft mc, ScaledResolution res, double now) {
        if (titleCombo == 0) {
            return;
        }
        double t = now - titleTick;
        if (t < 0 || t >= TITLE_GUARD_TICKS || MilestoneEffects.lastShownTick >= titleTick) {
            titleCombo = 0;
            return;
        }
        double alpha = t < TITLE_IN ? t / TITLE_IN
                : t < TITLE_IN + TITLE_STAY ? 1 : 1 - (t - TITLE_IN - TITLE_STAY) / TITLE_OUT;
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        if (a < 8) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        String text = TextFormatting.BOLD + I18n.format("weakspot.combo.title", titleCombo);
        GlStateManager.enableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.translate(res.getScaledWidth() / 2F, res.getScaledHeight() / 2F - 40, 0);
        GlStateManager.scale(TITLE_SCALE, TITLE_SCALE, 1);
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2F, -font.FONT_HEIGHT / 2F,
                a << 24 | ComboMilestones.glowRgb(ComboMilestones.GRAND_STEP));
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
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

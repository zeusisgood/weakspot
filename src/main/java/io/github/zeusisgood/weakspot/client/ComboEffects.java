package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboMilestones;
import io.github.zeusisgood.weakspot.common.HitPitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

/**
 * コンボの段階（10 / 25 / 50 / 100 / 250 / 500 / 1000）の演出（1.8.9 で ComboHud から分けた）: 強調音、和音、駆け上がり、
 * 花火、「1000 COMBO!」のタイトル。上の段階ほど足していく。数字と光は ComboHud。
 * 音は 1.9.5 から、ヒット音に埋もれないようベルで大きめに鳴らす（HitSounds.accentRun）。
 */
final class ComboEffects {

    /** 段階の演出の強調音を、ヒット音から少し遅らせる（tick）。 */
    private static final int ACCENT_DELAY_TICKS = 2;
    /** 100 の和音（ド・ミ・ソ・上のド。連続ヒット数で表した音階の位置）。 */
    private static final int[] CHORD = {1, 3, 5, HitPitch.SCALE_LENGTH};
    /** 10・25・50 の分散和音（ド・ミ・ソ）。 */
    private static final int[] ARPEGGIO = {1, 3, 5};
    /** 250 の駆け上がり（ドからソまで）。 */
    private static final int[] SHORT_RUN = {1, 2, 3, 4, 5};
    /** 1000 のタイトル（フェードイン、表示、フェードアウトの tick）。バニラのタイトルと同じ長さ。 */
    private static final int TITLE_IN = 5;
    private static final int TITLE_STAY = 50;
    private static final int TITLE_OUT = 20;
    /** 節目のタイトルがこの tick 以内に出ていたら、1000 のタイトルは出さない（節目を優先する）。 */
    private static final int TITLE_GUARD_TICKS = TITLE_IN + TITLE_STAY + TITLE_OUT;
    private static final float TITLE_SCALE = 3;

    /** 1000 のタイトルを出し始めた clientTick と、その数（0 なら出していない）。 */
    private static long titleTick;
    private static int titleCombo;

    private ComboEffects() {
    }

    /** 段階の演出。上の段階ほど足していく。 */
    static void playStep(int step) {
        if (step >= ComboMilestones.GRAND_STEP) {
            float[] run = new float[HitPitch.TWO_OCTAVE_LENGTH];
            for (int i = 0; i < run.length; i++) {
                run[i] = HitPitch.twoOctave(i);
            }
            HitSounds.accentRun(run, 1, ACCENT_DELAY_TICKS);
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
            HitSounds.accentScale(1, ACCENT_DELAY_TICKS);
            HitSounds.schedule(ACCENT_DELAY_TICKS,
                    () -> MilestoneEffects.comboFireworks(1, ComboMilestones.glowRgb(step)));
        } else if (step >= 250) {
            HitSounds.accentRun(pitches(SHORT_RUN), 1, ACCENT_DELAY_TICKS);
        } else if (step >= 100) {
            playChord(ACCENT_DELAY_TICKS);
        } else {
            HitSounds.accentRun(pitches(ARPEGGIO), 1, ACCENT_DELAY_TICKS);
        }
    }

    /** 和音（同時に鳴らす）。 */
    private static void playChord(int delay) {
        HitSounds.accentRun(pitches(CHORD), 0, delay);
    }

    /** 音階の位置（連続ヒット数で表したもの）のピッチ。 */
    private static float[] pitches(int[] notes) {
        float[] pitches = new float[notes.length];
        for (int i = 0; i < notes.length; i++) {
            pitches[i] = HitPitch.forStreak(notes[i]);
        }
        return pitches;
    }

    /** 1000（以降 1000 ごと）のタイトル。画面の中央の少し上に、金色で大きく出す。 */
    static void drawTitle(Minecraft mc, ScaledResolution res, double now) {
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

    /** ワールドを出た、死亡した、ディメンションを移動した。 */
    static void clear() {
        titleCombo = 0;
    }
}

package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboMilestones;
import io.github.zeusisgood.weakspot.common.HitScale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

/** 上の段階ほど演出を足していく。音は、ヒット音に埋もれないようベルで大きめに鳴らす（HitSounds.accentRun）。 */
final class ComboEffects {

    private static final int ACCENT_DELAY_TICKS = 2;
    /** 音階の度数（各自の音階の種類で数える）。 */
    private static final int[] ARPEGGIO = {0, 2, 4};
    private static final int[] SHORT_RUN = {0, 1, 2, 3, 4};
    /** バニラのタイトルと同じ長さ。 */
    private static final int TITLE_IN = 5;
    private static final int TITLE_STAY = 50;
    private static final int TITLE_OUT = 20;
    /** 節目のタイトルがこの tick 以内に出ていたら、コンボのタイトルは出さない（節目を優先する）。 */
    private static final int TITLE_GUARD_TICKS = TITLE_IN + TITLE_STAY + TITLE_OUT;
    private static final float TITLE_SCALE = 3;

    /** 0 なら出していない。 */
    private static long titleTick;
    private static int titleCombo;

    private ComboEffects() {
    }

    static void playStep(int step) {
        boolean grand = step == ComboMilestones.TITLE_FROM
                || step >= ComboMilestones.GRAND_STEP && step % ComboMilestones.GRAND_STEP == 0;
        if (grand) {
            HitScale scale = HitSounds.scale();
            float[] run = new float[scale.twoOctaveLength()];
            for (int i = 0; i < run.length; i++) {
                run[i] = scale.twoOctave(i);
            }
            HitSounds.accentRun(run, 1, ACCENT_DELAY_TICKS);
            playChord(ACCENT_DELAY_TICKS + run.length + 1);
            HitSounds.schedule(ACCENT_DELAY_TICKS, () -> {
                MilestoneEffects.comboFireworks(3, glowOf(step));
                showTitle(step);
            });
        } else if (step >= 200) {
            HitSounds.accentScale(1, ACCENT_DELAY_TICKS);
            HitSounds.schedule(ACCENT_DELAY_TICKS, () -> {
                MilestoneEffects.comboFireworks(1, glowOf(step));
                if (step >= ComboMilestones.TITLE_FROM) {
                    showTitle(step);
                }
            });
        } else if (step >= 150) {
            HitSounds.accentRun(pitches(SHORT_RUN), 1, ACCENT_DELAY_TICKS);
        } else if (step >= 100) {
            playChord(ACCENT_DELAY_TICKS);
        } else {
            HitSounds.accentRun(pitches(ARPEGGIO), 1, ACCENT_DELAY_TICKS);
        }
    }

    /** 虹色の段階は、今の数字の色。 */
    private static int glowOf(int step) {
        int rgb = ComboMilestones.glowRgb(step);
        return rgb >= 0 ? rgb : ComboHud.colorOf(step);
    }

    private static void showTitle(int step) {
        long now = ClientWeakSpotHandler.clientTick;
        if (now - MilestoneEffects.lastShownTick > TITLE_GUARD_TICKS) {
            titleTick = now;
            titleCombo = step;
        }
    }

    private static void playChord(int delay) {
        HitSounds.accentRun(pitches(new int[] {0, 2, 4, HitSounds.scale().perOctave()}), 0, delay);
    }

    static float[] pitches(int[] degrees) {
        HitScale scale = HitSounds.scale();
        float[] pitches = new float[degrees.length];
        for (int i = 0; i < degrees.length; i++) {
            pitches[i] = scale.pitchOfDegree(degrees[i]);
        }
        return pitches;
    }

    static void drawTitle(GuiGraphics g, Minecraft mc, double now) {
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
        Font font = mc.font;
        String text = ChatFormatting.BOLD + I18n.get("weakspot.combo.title", titleCombo);
        g.pose().pushPose();
        g.pose().translate(mc.getWindow().getGuiScaledWidth() / 2F, mc.getWindow().getGuiScaledHeight() / 2F - 40, 0);
        g.pose().scale(TITLE_SCALE, TITLE_SCALE, 1);
        g.drawString(font, text, -font.width(text) / 2F, -font.lineHeight / 2F, a << 24 | glowOf(titleCombo), true);
        g.pose().popPose();
    }

    static void clear() {
        titleCombo = 0;
    }
}

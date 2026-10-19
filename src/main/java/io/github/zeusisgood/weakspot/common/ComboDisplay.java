package io.github.zeusisgood.weakspot.common;

/** 時間は tick（途中の値も可）。 */
public final class ComboDisplay {

    public static final int MIN_SHOWN = 2;
    public static final int MIN_MAX_SHOWN = 5;
    public static final int MAX_HOLD_TICKS = 20;
    public static final int FADE_TICKS = 10;
    public static final int BOUNCE_TICKS = 4;
    public static final double BOUNCE_PEAK = 1.4;
    /** 150 以上の段階に達したヒット。 */
    public static final double BIG_BOUNCE_PEAK = 1.9;
    public static final int GLOW_TICKS = 12;

    private ComboDisplay() {
    }

    public static double bounceScale(double ticksSinceHit) {
        return bounceScale(ticksSinceHit, BOUNCE_PEAK);
    }

    public static double bounceScale(double ticksSinceHit, double peak) {
        if (ticksSinceHit < 0 || ticksSinceHit >= BOUNCE_TICKS) {
            return 1;
        }
        double t = 1 - ticksSinceHit / BOUNCE_TICKS;
        return 1 + (peak - 1) * t * t;
    }

    public static boolean showsMax(int brokenCombo) {
        return brokenCombo >= MIN_MAX_SHOWN;
    }

    /** 1〜0。 */
    public static double fadeAlpha(int brokenCombo, double ticksSinceBreak) {
        double hold = showsMax(brokenCombo) ? MAX_HOLD_TICKS : 0;
        return Math.max(0, Math.min(1, 1 - (ticksSinceBreak - hold) / FADE_TICKS));
    }

    /** 1〜0。 */
    public static double glowAlpha(double ticksSinceStep) {
        if (ticksSinceStep < 0) {
            return 0;
        }
        return Math.max(0, 1 - ticksSinceStep / GLOW_TICKS);
    }

    /** 100 ごとに速く流れる（800 ミリ秒まで）。 */
    public static long rainbowPeriodMs(int combo) {
        int hundreds = Math.max(0, combo / 100 - 4);
        return Math.max(800, 3000 - 300L * hundreds);
    }
}

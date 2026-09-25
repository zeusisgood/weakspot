package com.example.weakspot.common;

/** コンボの表示の時間の計算（弾む動き、途切れたあとの消え方、段階の演出の光）。時間は tick（途中の値も可）。 */
public final class ComboDisplay {

    /** 表示する最小のコンボ。 */
    public static final int MIN_SHOWN = 2;
    /** 途切れたときに「MAX」を残す最小のコンボ。 */
    public static final int MIN_MAX_SHOWN = 5;
    /** 「MAX」を残す時間。 */
    public static final int MAX_HOLD_TICKS = 20;
    /** 薄くなって消えるまでの時間。 */
    public static final int FADE_TICKS = 10;
    /** ヒットで大きくなってから戻るまでの時間。 */
    public static final int BOUNCE_TICKS = 4;
    /** ヒットした瞬間の大きさ（1 に戻る）。 */
    public static final double BOUNCE_PEAK = 1.4;
    /** 250 以上の段階に達したヒットの大きさ（大きく弾ませる）。 */
    public static final double BIG_BOUNCE_PEAK = 1.9;
    /** 段階の演出の光が消えるまでの時間。 */
    public static final int GLOW_TICKS = 12;

    private ComboDisplay() {
    }

    /** ヒットからの経過に応じた大きさ。ヒットの瞬間が BOUNCE_PEAK で、BOUNCE_TICKS で 1 に戻る。 */
    public static double bounceScale(double ticksSinceHit) {
        return bounceScale(ticksSinceHit, BOUNCE_PEAK);
    }

    /** ヒットからの経過に応じた大きさ。ヒットの瞬間が peak で、BOUNCE_TICKS で 1 に戻る。 */
    public static double bounceScale(double ticksSinceHit, double peak) {
        if (ticksSinceHit < 0 || ticksSinceHit >= BOUNCE_TICKS) {
            return 1;
        }
        double t = 1 - ticksSinceHit / BOUNCE_TICKS;
        return 1 + (peak - 1) * t * t;
    }

    /** 途切れたコンボで「MAX」を残すか。 */
    public static boolean showsMax(int brokenCombo) {
        return brokenCombo >= MIN_MAX_SHOWN;
    }

    /** 途切れてからの濃さ（1〜0）。「MAX」を残すときは MAX_HOLD_TICKS のあいだ濃いまま、その後 FADE_TICKS で消える。 */
    public static double fadeAlpha(int brokenCombo, double ticksSinceBreak) {
        double hold = showsMax(brokenCombo) ? MAX_HOLD_TICKS : 0;
        return Math.max(0, Math.min(1, 1 - (ticksSinceBreak - hold) / FADE_TICKS));
    }

    /** 段階の演出の光の濃さ（1〜0）。 */
    public static double glowAlpha(double ticksSinceStep) {
        if (ticksSinceStep < 0) {
            return 0;
        }
        return Math.max(0, 1 - ticksSinceStep / GLOW_TICKS);
    }

    /** 虹色（100 以上）が一周する時間（ミリ秒）。段階が上がるほど速く流れる。 */
    public static long rainbowPeriodMs(int combo) {
        if (combo >= 1000) {
            return 1200;
        }
        if (combo >= 500) {
            return 2000;
        }
        if (combo >= 250) {
            return 3000;
        }
        return 4000;
    }
}

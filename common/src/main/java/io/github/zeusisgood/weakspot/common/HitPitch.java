package io.github.zeusisgood.weakspot.common;

/** 初期値の音階（長音階・上がって戻る・1 オクターブ）のヒット音。各自の音階は HitScale。 */
public final class HitPitch {

    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11, 12};
    /** この数のヒットで最高音になる。 */
    public static final int SCALE_LENGTH = MAJOR_SCALE.length;

    private HitPitch() {
    }

    /** streak は 1 始まり。 */
    public static float forStreak(int streak) {
        return HitScale.DEFAULT.melody(streak);
    }

    public static final int BREAK_SOUND_FROM = 10;

    /** 和音の形（旋律から何度下を重ねるか。音階の度数）。コンボの段階 ComboTier に合わせる。 */
    private static final int[] THIRD = {-2};
    private static final int[] TRIAD = {-2, -4};
    private static final int[] TRIAD_OCTAVE = {-2, -4, -7};
    private static final int[] SIXTH = {-2, -4, -5};
    private static final int[] SEVENTH = {-2, -4, -6};
    private static final int[] NINTH = {-2, -4, -6, -8};
    private static final int[] SUS4 = {-3, -4};
    /** 400 から 100 ごとに巡る。 */
    private static final int[][] CYCLE = {SIXTH, SEVENTH, NINTH, SUS4};
    public static final int BELL_FROM = 300;

    /** 長音階で数える（ほかの音階では HitScale が数え直す）。 */
    static int[] chordOffsets(int streak) {
        if (streak >= 400) {
            return CYCLE[(streak / 100 - 4) % CYCLE.length];
        }
        if (streak >= 300) {
            return NINTH;
        }
        if (streak >= 250) {
            return SUS4;
        }
        if (streak >= 200) {
            return NINTH;
        }
        if (streak >= 150) {
            return SEVENTH;
        }
        if (streak >= 100) {
            return SIXTH;
        }
        if (streak >= 75) {
            return TRIAD_OCTAVE;
        }
        if (streak >= 50) {
            return TRIAD;
        }
        if (streak >= 25) {
            return THIRD;
        }
        return new int[0];
    }

    static boolean isSus4(int streak) {
        return chordOffsets(streak) == SUS4;
    }

    /** 重ねる音は旋律より下（ピッチの上限 2.0 のため）。chords が false なら旋律だけ。 */
    public static float[] forHit(int streak, boolean chords) {
        return HitScale.DEFAULT.forHit(streak, chords);
    }

    /** sus4 の形でなければ 0。 */
    public static float resolveFor(int streak, boolean chords) {
        return HitScale.DEFAULT.resolveFor(streak, chords);
    }

    public static boolean withBell(int streak, boolean chords) {
        return chords && streak >= BELL_FROM;
    }

    public static float[] breakNotes() {
        return HitScale.DEFAULT.breakNotes();
    }

    /** ピッチ 0.5 の低いドから 2.0 の高いドまで。 */
    public static final int TWO_OCTAVE_LENGTH = MAJOR_SCALE.length * 2 - 1;

    /** index は 0 始まり。 */
    public static float twoOctave(int index) {
        return HitScale.DEFAULT.twoOctave(index);
    }
}

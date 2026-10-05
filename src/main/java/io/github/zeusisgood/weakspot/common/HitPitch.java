package io.github.zeusisgood.weakspot.common;

/**
 * 連続ヒット数（HitStreak。戻らずに上がり続ける）に応じたヒット音のピッチ。
 * 長音階で1オクターブ上がり、上がりきったら最低音に戻って繰り返す（初期値。1.11.1 から各自の音階は HitScale）。
 */
public final class HitPitch {

    /** 長音階（ド レ ミ ファ ソ ラ シ ド）の半音数。 */
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11, 12};
    /** 音階の1オクターブの音の数。この数のヒットで最高音になる。 */
    public static final int SCALE_LENGTH = MAJOR_SCALE.length;

    private HitPitch() {
    }

    /** streak は 1 始まり（1回目のヒットが最低音、9回目で再び最低音）。初期値の音階（HitScale.DEFAULT）で数える。 */
    public static float forStreak(int streak) {
        return HitScale.DEFAULT.melody(streak);
    }

    /** このコンボ以上で途切れたとき、下がる 2 音を鳴らす（1.9.5）。 */
    public static final int BREAK_SOUND_FROM = 10;

    /** 和音の形（旋律から何度下を重ねるか。音階の度数）。1.11.0 でコンボの段階（ComboTier）に合わせて増やした。 */
    private static final int[] THIRD = {-2};
    private static final int[] TRIAD = {-2, -4};
    private static final int[] TRIAD_OCTAVE = {-2, -4, -7};
    private static final int[] SIXTH = {-2, -4, -5};
    private static final int[] SEVENTH = {-2, -4, -6};
    private static final int[] NINTH = {-2, -4, -6, -8};
    private static final int[] SUS4 = {-3, -4};
    /** 400 から 100 ごとに巡る形。 */
    private static final int[][] CYCLE = {SIXTH, SEVENTH, NINTH, SUS4};
    /** このコンボから、旋律にベルを重ねる。 */
    public static final int BELL_FROM = 300;

    /** そのコンボで旋律の下に重ねる度数（1.11.0。長音階で数える。ほかの音階では HitScale が数え直す）。 */
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

    /** sus4 の形か（解決の音を鳴らす。1.11.0）。 */
    static boolean isSus4(int streak) {
        return chordOffsets(streak) == SUS4;
    }

    /**
     * 1 回のヒットで鳴らすピッチ（1.9.5）。先頭が旋律（forStreak）、続けて重ねる音（旋律より下。ピッチの上限 2.0 のため）。
     * 1.11.0 から、重ねる形はコンボの段階ごと（chordOffsets）。下のドより低くなる音は 1 オクターブ上げる（下限 0.5 のため）。
     * chords が false なら旋律だけ。初期値の音階で数える（各自の音階は HitScale.forHit。1.11.1）。
     */
    public static float[] forHit(int streak, boolean chords) {
        return HitScale.DEFAULT.forHit(streak, chords);
    }

    /** sus4 の形のとき、少し遅れて鳴らす解決の音（なければ 0）。 */
    public static float resolveFor(int streak, boolean chords) {
        return HitScale.DEFAULT.resolveFor(streak, chords);
    }

    /** 旋律にベルを重ねるか（300 から）。 */
    public static boolean withBell(int streak, boolean chords) {
        return chords && streak >= BELL_FROM;
    }

    /** コンボが途切れたときの下がる 2 音（下のソ → 下のド。1.9.5）。 */
    public static float[] breakNotes() {
        return HitScale.DEFAULT.breakNotes();
    }

    /** 2 オクターブの駆け上がり（ピッチ 0.5 の低いドから 2.0 の高いドまで）の音の数。 */
    public static final int TWO_OCTAVE_LENGTH = MAJOR_SCALE.length * 2 - 1;

    /** 2 オクターブの駆け上がりの index 番目（0 始まり）の音のピッチ。 */
    public static float twoOctave(int index) {
        return HitScale.DEFAULT.twoOctave(index);
    }
}

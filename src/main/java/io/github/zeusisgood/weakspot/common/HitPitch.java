package io.github.zeusisgood.weakspot.common;

/**
 * 連続ヒット数（HitStreak。戻らずに上がり続ける）に応じたヒット音のピッチ。
 * 長音階で1オクターブ上がり、上がりきったら最低音に戻って繰り返す。
 */
public final class HitPitch {

    /** 長音階（ド レ ミ ファ ソ ラ シ ド）の半音数。 */
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11, 12};
    /** 音階の1オクターブの音の数。この数のヒットで最高音になる。 */
    public static final int SCALE_LENGTH = MAJOR_SCALE.length;
    /** Minecraft の音のピッチは 0.5〜2.0。1.0 から始めて最高の 2.0 で1オクターブになる。 */
    private static final double BASE_PITCH = 1.0;

    private HitPitch() {
    }

    /** streak は 1 始まり（1回目のヒットが最低音、9回目で再び最低音）。 */
    public static float forStreak(int streak) {
        int index = (Math.max(streak, 1) - 1) % MAJOR_SCALE.length;
        return (float) (BASE_PITCH * Math.pow(2, MAJOR_SCALE[index] / 12.0));
    }

    /** 長音階の 1 オクターブの 7 音（上のドを除く）。音階の何度下かを数えるのに使う。 */
    private static final int[] SEVEN = {0, 2, 4, 5, 7, 9, 11};
    /** このコンボから、旋律の 3 度下を重ねる（1.9.5。コンボの数字の橙と同じ区切り）。 */
    public static final int TWO_NOTES_FROM = 25;
    /** このコンボから、さらに 5 度下も重ねる（1.9.5。コンボの数字の虹色と同じ区切り）。 */
    public static final int THREE_NOTES_FROM = 100;
    /** このコンボ以上で途切れたとき、下がる 2 音を鳴らす（1.9.5）。 */
    public static final int BREAK_SOUND_FROM = 10;

    /**
     * 1 回のヒットで鳴らすピッチ（1.9.5）。先頭が旋律（forStreak）、続けて重ねる音（旋律より下。ピッチの上限 2.0 のため）。
     * コンボ 1〜24 は旋律だけ、25〜99 は 3 度下（音階で 2 つ下）も、100 以上は 5 度下（4 つ下）も。chords が false なら旋律だけ。
     */
    public static float[] forHit(int streak, boolean chords) {
        int degree = (Math.max(streak, 1) - 1) % MAJOR_SCALE.length;
        int notes = !chords || streak < TWO_NOTES_FROM ? 1 : streak < THREE_NOTES_FROM ? 2 : 3;
        float[] pitches = new float[notes];
        for (int i = 0; i < notes; i++) {
            pitches[i] = pitchOfDegree(degree - 2 * i);
        }
        return pitches;
    }

    /** コンボが途切れたときの下がる 2 音（下のソ → 下のド。1.9.5）。 */
    public static float[] breakNotes() {
        return new float[] {pitchOfDegree(-3), pitchOfDegree(-7)};
    }

    /** 1.0 のドを 0 とした音階の度数（負なら下のオクターブ）のピッチ。 */
    static float pitchOfDegree(int degree) {
        int octave = Math.floorDiv(degree, SEVEN.length);
        int semitones = SEVEN[Math.floorMod(degree, SEVEN.length)] + 12 * octave;
        return (float) (BASE_PITCH * Math.pow(2, semitones / 12.0));
    }

    /** 2 オクターブの駆け上がり（ピッチ 0.5 の低いドから 2.0 の高いドまで）の音の数。 */
    public static final int TWO_OCTAVE_LENGTH = MAJOR_SCALE.length * 2 - 1;

    /** 2 オクターブの駆け上がりの index 番目（0 始まり）の音のピッチ。 */
    public static float twoOctave(int index) {
        int i = Math.max(0, Math.min(index, TWO_OCTAVE_LENGTH - 1));
        int semitones = i < MAJOR_SCALE.length ? MAJOR_SCALE[i] : 12 + MAJOR_SCALE[i - MAJOR_SCALE.length + 1];
        return (float) (BASE_PITCH / 2 * Math.pow(2, semitones / 12.0));
    }
}

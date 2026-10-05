package io.github.zeusisgood.weakspot.common;

/**
 * ヒット音の音階（1.11.1。各自の設定 hitScaleType / hitScaleDirection / hitScaleOctaves）。音階の種類（長音階・
 * ペンタトニック・短調）、動き（上がって戻る・往復）、音域（1〜2 オクターブ）から、連続ヒット数のピッチ・和音・
 * 節目の駆け上がり・途切れたときの音を決める。度数 0 がピッチ 1.0 のド（負なら下のオクターブ。ピッチの下限 0.5 は -n）。
 * DEFAULT（長音階・上がって戻る・1 オクターブ）は 1.11.0 までと同じ鳴り方（HitPitch の静的な関数はこれを使う）。
 */
public final class HitScale {

    /** 音階の種類。steps は 1 オクターブの中の音の半音数（上のドを除く）。 */
    public enum Type {
        MAJOR(0, 2, 4, 5, 7, 9, 11),
        PENTATONIC(0, 2, 4, 7, 9),
        /** 自然的短音階。 */
        MINOR(0, 2, 3, 5, 7, 8, 10);

        final int[] steps;

        Type(int... steps) {
            this.steps = steps;
        }

        /** 設定画面の切り替えボタン用。最後の次は最初に戻る。 */
        public Type next() {
            Type[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    /** 音階の動き。UP は上がりきったら最低音に戻る、UP_DOWN は上がったら下がる往復。 */
    public enum Direction {
        UP,
        UP_DOWN;

        public Direction next() {
            Direction[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    public static final int MIN_OCTAVES = 1;
    public static final int MAX_OCTAVES = 2;
    /** 1.11.0 までと同じ鳴り方。 */
    public static final HitScale DEFAULT = new HitScale(Type.MAJOR, Direction.UP, 1);

    /** 長音階で数えた和音の度数のうち、1 オクターブ下（-7）以下のもの。ほかの音階では、その音階の 1 オクターブに合わせる。 */
    private static final int HEPTATONIC = 7;
    /** sus4 の解決の音（旋律の 2 度下。長音階で数えた度数）。 */
    private static final int RESOLVE = -2;
    /** 完全 5 度の半音数（途切れたときの「下のソ」）。 */
    private static final int FIFTH = 7;

    public final Type type;
    public final Direction direction;
    public final int octaves;

    public HitScale(Type type, Direction direction, int octaves) {
        this.type = type == null ? Type.MAJOR : type;
        this.direction = direction == null ? Direction.UP : direction;
        this.octaves = Math.max(MIN_OCTAVES, Math.min(MAX_OCTAVES, octaves));
    }

    /** 1 オクターブの音の数（上のドを除く）。 */
    public int perOctave() {
        return type.steps.length;
    }

    /** 旋律の並びの音の数（最低音から最高音まで。1 オクターブの長音階なら 8）。 */
    public int melodyLength() {
        return perOctave() * octaves + 1;
    }

    /** 旋律が 1 周する連続ヒット数（上がって戻るなら melodyLength、往復なら 2 × (melodyLength − 1)）。 */
    public int cycleLength() {
        int length = melodyLength();
        return direction == Direction.UP_DOWN ? 2 * (length - 1) : length;
    }

    /** 連続ヒット数（1 始まり）の旋律の度数。 */
    public int melodyDegree(int streak) {
        int length = melodyLength();
        int k = (Math.max(streak, 1) - 1) % cycleLength();
        int index = direction == Direction.UP_DOWN && k >= length ? cycleLength() - k : k;
        int lowest = octaves == 2 ? -perOctave() : 0;
        return lowest + index;
    }

    /** 連続ヒット数の旋律のピッチ。 */
    public float melody(int streak) {
        return pitchOfDegree(melodyDegree(streak));
    }

    /**
     * 1 回のヒットで鳴らすピッチ。先頭が旋律、続けて重ねる音（コンボの段階ごとの形 HitPitch.chordOffsets を、この音階の
     * 中で数える）。ピッチ 0.5 より低くなる音は、0.5 以上になるまでオクターブ上げる。chords が false なら旋律だけ。
     */
    public float[] forHit(int streak, boolean chords) {
        int degree = melodyDegree(streak);
        int[] offsets = chords ? HitPitch.chordOffsets(streak) : new int[0];
        float[] pitches = new float[1 + offsets.length];
        pitches[0] = pitchOfDegree(degree);
        for (int i = 0; i < offsets.length; i++) {
            pitches[i + 1] = pitchOfDegree(lifted(degree + offset(offsets[i])));
        }
        return pitches;
    }

    /** sus4 の形のとき、少し遅れて鳴らす解決の音（なければ 0）。 */
    public float resolveFor(int streak, boolean chords) {
        if (!chords || !HitPitch.isSus4(streak)) {
            return 0;
        }
        return pitchOfDegree(lifted(melodyDegree(streak) + offset(RESOLVE)));
    }

    /** コンボが途切れたときの下がる 2 音（下の 5 度 → 下のド。ペンタトニック・短調でも 5 度はある）。 */
    public float[] breakNotes() {
        int n = perOctave();
        return new float[] {pitchOfDegree(-n + indexOf(FIFTH)), pitchOfDegree(-n)};
    }

    /** 1 オクターブの音階（ドから上のドまで）。節目の音階の駆け上がり。 */
    public float[] oneOctave() {
        float[] pitches = new float[perOctave() + 1];
        for (int i = 0; i < pitches.length; i++) {
            pitches[i] = pitchOfDegree(i);
        }
        return pitches;
    }

    /** 2 オクターブの駆け上がり（ピッチ 0.5 の低いドから 2.0 の高いドまで）の音の数。 */
    public int twoOctaveLength() {
        return perOctave() * 2 + 1;
    }

    /** 2 オクターブの駆け上がりの index 番目（0 始まり）のピッチ。 */
    public float twoOctave(int index) {
        int i = Math.max(0, Math.min(index, twoOctaveLength() - 1));
        return pitchOfDegree(-perOctave() + i);
    }

    /** 度数のピッチ（0 がピッチ 1.0 のド）。 */
    public float pitchOfDegree(int degree) {
        int n = perOctave();
        int octave = Math.floorDiv(degree, n);
        int semitones = type.steps[Math.floorMod(degree, n)] + 12 * octave;
        return (float) Math.pow(2, semitones / 12.0);
    }

    /** 長音階で数えた和音の度数を、この音階の度数にする（1 オクターブ下より下は、この音階の 1 オクターブで数える）。 */
    private int offset(int heptatonic) {
        return heptatonic <= -HEPTATONIC ? heptatonic + HEPTATONIC - perOctave() : heptatonic;
    }

    /** ピッチ 0.5（度数 -n）より低い度数を、オクターブ上げる。 */
    private int lifted(int degree) {
        int n = perOctave();
        int d = degree;
        while (d < -n) {
            d += n;
        }
        return d;
    }

    private int indexOf(int semitones) {
        for (int i = 0; i < type.steps.length; i++) {
            if (type.steps[i] == semitones) {
                return i;
            }
        }
        return 0;
    }
}

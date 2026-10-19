package io.github.zeusisgood.weakspot.common;

/**
 * ヒット音の音階（各自の設定 hitScaleType / hitScaleDirection / hitScaleOctaves）。
 * 度数 0 がピッチ 1.0 のド（負なら下のオクターブ。ピッチの下限 0.5 は -n）。DEFAULT は HitPitch と同じ鳴り方。
 */
public final class HitScale {

    /** steps は 1 オクターブの中の音の半音数（上のドを除く）。 */
    public enum Type {
        MAJOR(0, 2, 4, 5, 7, 9, 11),
        PENTATONIC(0, 2, 4, 7, 9),
        /** 自然的短音階。 */
        MINOR(0, 2, 3, 5, 7, 8, 10);

        final int[] steps;

        Type(int... steps) {
            this.steps = steps;
        }

        public Type next() {
            Type[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    /** UP は上がりきったら最低音に戻り、UP_DOWN は上がったら下がる。 */
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

    /** 上のドを除く。 */
    public int perOctave() {
        return type.steps.length;
    }

    /** 最低音から最高音まで（1 オクターブの長音階なら 8）。 */
    public int melodyLength() {
        return perOctave() * octaves + 1;
    }

    public int cycleLength() {
        int length = melodyLength();
        return direction == Direction.UP_DOWN ? 2 * (length - 1) : length;
    }

    /** streak は 1 始まり。 */
    public int melodyDegree(int streak) {
        int length = melodyLength();
        int k = (Math.max(streak, 1) - 1) % cycleLength();
        int index = direction == Direction.UP_DOWN && k >= length ? cycleLength() - k : k;
        int lowest = octaves == 2 ? -perOctave() : 0;
        return lowest + index;
    }

    public float melody(int streak) {
        return pitchOfDegree(melodyDegree(streak));
    }

    /** 先頭が旋律、続けて重ねる音。chords が false なら旋律だけ。 */
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

    /** sus4 の形でなければ 0。 */
    public float resolveFor(int streak, boolean chords) {
        if (!chords || !HitPitch.isSus4(streak)) {
            return 0;
        }
        return pitchOfDegree(lifted(melodyDegree(streak) + offset(RESOLVE)));
    }

    public float[] breakNotes() {
        int n = perOctave();
        return new float[] {pitchOfDegree(-n + indexOf(FIFTH)), pitchOfDegree(-n)};
    }

    public float[] oneOctave() {
        float[] pitches = new float[perOctave() + 1];
        for (int i = 0; i < pitches.length; i++) {
            pitches[i] = pitchOfDegree(i);
        }
        return pitches;
    }

    /** 2 オクターブの駆け上がりは、ピッチ 0.5 の低いドから 2.0 の高いドまで。 */
    public int twoOctaveLength() {
        return perOctave() * 2 + 1;
    }

    /** index は 0 始まり。 */
    public float twoOctave(int index) {
        int i = Math.max(0, Math.min(index, twoOctaveLength() - 1));
        return pitchOfDegree(-perOctave() + i);
    }

    public float pitchOfDegree(int degree) {
        int n = perOctave();
        int octave = Math.floorDiv(degree, n);
        int semitones = type.steps[Math.floorMod(degree, n)] + 12 * octave;
        return (float) Math.pow(2, semitones / 12.0);
    }

    /** 長音階で数えた度数を、この音階の度数にする。 */
    private int offset(int heptatonic) {
        return heptatonic <= -HEPTATONIC ? heptatonic + HEPTATONIC - perOctave() : heptatonic;
    }

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

package com.example.weakspot.common;

/** 連続ヒット数に応じたヒット音のピッチ。長音階で1オクターブ上がり、上がりきったら最低音に戻って繰り返す。 */
public final class HitPitch {

    /** 長音階（ド レ ミ ファ ソ ラ シ ド）の半音数。 */
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11, 12};
    /** Minecraft の音のピッチは 0.5〜2.0。1.0 から始めて最高の 2.0 で1オクターブになる。 */
    private static final double BASE_PITCH = 1.0;

    private HitPitch() {
    }

    /** streak は 1 始まり（1回目のヒットが最低音、9回目で再び最低音）。 */
    public static float forStreak(int streak) {
        int index = (Math.max(streak, 1) - 1) % MAJOR_SCALE.length;
        return (float) (BASE_PITCH * Math.pow(2, MAJOR_SCALE[index] / 12.0));
    }
}

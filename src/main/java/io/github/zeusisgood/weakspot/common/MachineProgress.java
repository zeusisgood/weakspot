package io.github.zeusisgood.weakspot.common;

/** どれも 0〜1 に収める。値が読めない・0 除算のときは 0。かまど・醸造台の値は IInventory#getField、スポナーは spawnDelay。 */
public final class MachineProgress {

    /** バニラの醸造時間（tick）。 */
    public static final int BREW_TICKS = 400;
    /** バニラの最大（ブレイズパウダー 1 個分）。 */
    public static final int BREW_FUEL_MAX = 20;

    private MachineProgress() {
    }

    /** max が 0 以下なら 0。 */
    public static double ratio(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, value / (double) max));
    }

    /** brewTime は残りの tick。0 なら醸造していない。 */
    public static double brewing(int brewTime) {
        return brewTime <= 0 ? 0 : Math.max(0, Math.min(1, 1 - brewTime / (double) BREW_TICKS));
    }

    /**
    * スポナーの待ち時間は湧くたびにランダムに決め直されるので、始まりの値を覚える（前の値より増えたら、そこが始まり）。
    * 位置ごとに 1 つ持つ。
    */
    public static final class SpawnerDelay {

        private int start = -1;
        private int last = -1;

        public double update(int delay) {
            if (start < 0 || delay > last) {
                start = delay;
            }
            last = delay;
            return start <= 0 ? 0 : Math.max(0, Math.min(1, 1 - delay / (double) start));
        }
    }
}

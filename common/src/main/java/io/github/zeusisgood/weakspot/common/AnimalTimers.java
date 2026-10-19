package io.github.zeusisgood.weakspot.common;

/** 1 ヒット = 本来の時間 ÷ 目安のヒット数。 */
public final class AnimalTimers {

    /** EntityAgeable の初期値。 */
    public static final int BABY_TICKS = 24000;
    /** バニラの動物と村人。 */
    public static final int BREEDING_TICKS = 6000;
    /** 平均（バニラのニワトリは 6000〜11999 tick）。 */
    public static final int EGG_TICKS = 9000;

    /** 並びは、足元のバーに出す順（先が優先）。 */
    public enum Timer {
        BABY, WOOL, TRADE, BREEDING, EGG;

        public int bit() {
            return 1 << ordinal();
        }
    }

    private AnimalTimers() {
    }

    /** hits が 0 以下の設定は、その対象を無効として 0 を返す。 */
    public static int ticksPerHit(int fullTicks, int hits) {
        return hits <= 0 ? 0 : Math.max(1, fullTicks / hits);
    }

    /** age は負（子ども）。0 を超えない。子どもでなければそのまま。 */
    public static int babyAfterHit(int age, int ticks) {
        return age < 0 ? Math.min(0, age + ticks) : age;
    }

    /** age は正（待ち時間中）。0 を下回らない。 */
    public static int breedingAfterHit(int age, int ticks) {
        return age > 0 ? Math.max(0, age - ticks) : age;
    }

    /** 0 以下になったら、バニラが次の tick に卵を産む。 */
    public static int eggAfterHit(int remaining, int ticks) {
        return remaining - ticks;
    }

    /** 0〜1 を返す。age は -24000 が始まり。 */
    public static double babyProgress(int age) {
        return clamp(1.0 + (double) age / BABY_TICKS);
    }

    /** age は 6000 が始まり。 */
    public static double breedingProgress(int age) {
        return clamp(1.0 - (double) age / BREEDING_TICKS);
    }

    public static double eggProgress(int remaining) {
        return clamp(1.0 - (double) remaining / EGG_TICKS);
    }

    public static double hitProgress(int count, int needed) {
        return needed <= 0 ? 0.0 : clamp((double) count / needed);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** mask は動いているタイマーの Timer#bit の合計。なければ null。 */
    public static Timer barTimer(int mask) {
        for (Timer timer : Timer.values()) {
            if ((mask & timer.bit()) != 0) {
                return timer;
            }
        }
        return null;
    }

    /** 羊毛・取引上限のような、タイマーのない効果。必要なヒット数に達したヒットで 1 回だけ効果が出て、数え直す。 */
    public static final class HitCounter {

        private int count;

        /** 効果が出るなら true。needed が 0 以下なら、効果は出ない（無効）。 */
        public boolean hit(int needed) {
            if (needed <= 0) {
                return false;
            }
            count++;
            if (count >= needed) {
                count = 0;
                return true;
            }
            return false;
        }

        public int count() {
            return count;
        }

        public void reset() {
            count = 0;
        }
    }

    /**
    * 素手の右クリックにバニラの別の動作がある動物（乗る・座る・取引の画面）は、しゃがんで素手のときだけ弱点を出す。
    * listed: 設定 animalSneakRequiredEntities にある。
    */
    public static boolean requiresSneak(boolean horseLike, boolean tamed, boolean saddledPig, boolean villager,
                                        boolean listed) {
        return horseLike || tamed || saddledPig || villager || listed;
    }
}

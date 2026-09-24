package com.example.weakspot.common;

/**
 * 動物の弱点で進めるタイマーの計算（SPEC_v1.2 §1.9）。Minecraft に依存しない。
 * 「1ヒット = 本来の時間 ÷ 目安のヒット数」に換算する（基準は 1ヒット = 600 tick）。
 */
public final class AnimalTimers {

    /** 子どもが大人になるまでの本来の時間（EntityAgeable の初期値）。 */
    public static final int BABY_TICKS = 24000;
    /** 繁殖したあとの待ち時間（バニラの動物と村人）。 */
    public static final int BREEDING_TICKS = 6000;
    /** 次の卵までの平均の時間（バニラのニワトリは 6000〜11999 tick）。 */
    public static final int EGG_TICKS = 9000;

    /** 足元のバーに出す順番（先に書いたものを優先する）: 子どもの成長 → 羊毛 → 取引上限 → 繁殖の待ち時間 → 卵。 */
    public enum Timer {
        BABY, WOOL, TRADE, BREEDING, EGG;

        public int bit() {
            return 1 << ordinal();
        }
    }

    private AnimalTimers() {
    }

    /** 1ヒットで進める tick 数。ヒット数が 0 以下の設定は、その対象を無効として 0 を返す。 */
    public static int ticksPerHit(int fullTicks, int hits) {
        return hits <= 0 ? 0 : Math.max(1, fullTicks / hits);
    }

    /** 子どもの成長の値（負）を 0 に向けて進める。0 を超えない。子どもでなければそのまま。 */
    public static int babyAfterHit(int age, int ticks) {
        return age < 0 ? Math.min(0, age + ticks) : age;
    }

    /** 繁殖の待ち時間（正）を 0 に向けて減らす。0 を下回らない。待ち時間中でなければそのまま。 */
    public static int breedingAfterHit(int age, int ticks) {
        return age > 0 ? Math.max(0, age - ticks) : age;
    }

    /** 次の卵までの時間を減らす（0 以下になったら、バニラが次の tick に卵を産む）。 */
    public static int eggAfterHit(int remaining, int ticks) {
        return remaining - ticks;
    }

    /** 子どもの成長の進み具合（0.0〜1.0）。age は 0 未満（-24000 が始まり）。 */
    public static double babyProgress(int age) {
        return clamp(1.0 + (double) age / BABY_TICKS);
    }

    /** 繁殖の待ち時間の進み具合。age は 0 より大きい（6000 が始まり）。 */
    public static double breedingProgress(int age) {
        return clamp(1.0 - (double) age / BREEDING_TICKS);
    }

    /** 次の卵までの進み具合。remaining は次の卵までの残り tick。 */
    public static double eggProgress(int remaining) {
        return clamp(1.0 - (double) remaining / EGG_TICKS);
    }

    /** 羊毛・取引上限の進み具合（ヒット数 ÷ 必要なヒット数）。 */
    public static double hitProgress(int count, int needed) {
        return needed <= 0 ? 0.0 : clamp((double) count / needed);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * 足元のバーに出す1本。mask（動いているタイマーの Timer#bit の合計）のうち、表示の順番が一番先のもの。
     * 動いているものがなければ null。
     */
    public static Timer barTimer(int mask) {
        for (Timer timer : Timer.values()) {
            if ((mask & timer.bit()) != 0) {
                return timer;
            }
        }
        return null;
    }

    /**
     * 羊毛・取引上限のような、タイマーのない効果の、ヒット数の数え方。
     * 必要なヒット数に達したヒットで1回だけ効果が出て、数え直しになる。
     */
    public static final class HitCounter {

        private int count;

        /** ヒットした。効果が出るなら true（数え直す）。needed が 0 以下なら、効果は出ない（無効）。 */
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
     * 素手の右クリックにバニラの別の動作がある動物か（しゃがみ+素手のときだけ弱点を出す）。
     * horseLike: 馬・ロバ・ラバ・ラマなど（乗る、持ち物の画面）、tamed: 飼いならした EntityTameable（座る・立つ）、
     * saddledPig: サドルを付けた豚（乗る）、villager: 村人（取引の画面）、listed: 設定 animalSneakRequiredEntities にある。
     */
    public static boolean requiresSneak(boolean horseLike, boolean tamed, boolean saddledPig, boolean villager,
                                        boolean listed) {
        return horseLike || tamed || saddledPig || villager || listed;
    }
}

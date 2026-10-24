package io.github.zeusisgood.weakspot.common;

/** 欠片・砂時計・お守り・片眼鏡の数字。 */
public final class ItemRules {

    /** %。設定 shardChance の初期値。 */
    public static final double DEFAULT_SHARD_CHANCE = 0.5;

    /** コンボの猶予の効果がある間の、途切れるまでの tick。 */
    public static final int GRACE_RESET_TICKS = 120;
    /** 砂時計 1 つで足す効果の tick。 */
    public static final int HOURGLASS_TICKS = 6000;

    /** お守りの、弱点の大きさの倍率（spotSize と掛け合わせる）。 */
    public static final double CHARM_SIZE = 1.25;
    /** お守り・片眼鏡の耐久（ヒットの回数）。金床の修理はバニラと同じく 1 個で 1/4 戻る。 */
    public static final int TRINKET_DURABILITY = 500;

    /** 片眼鏡の予告の輪郭の濃さ。 */
    public static final float FORECAST_ALPHA = 0.35F;

    private ItemRules() {
    }

    /** 0〜（上限なし。1 以上なら毎回）。percent は設定の %、combo はヒット後のサーバーのコンボ数。 */
    public static double shardChance(double percent, int combo) {
        return Math.max(0, percent) / 100.0 * ComboFactor.factor(combo);
    }

    /** roll は 0 以上 1 未満の乱数。 */
    public static boolean dropsShard(double percent, int combo, double roll) {
        return roll < shardChance(percent, combo);
    }

    public static int comboResetTicks(boolean grace) {
        return grace ? GRACE_RESET_TICKS : HitStreak.RESET_TICKS;
    }

    /** 残りに足す（上限なし。int を超える分は止める）。 */
    public static int hourglassDuration(int remainingTicks) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, remainingTicks) + HOURGLASS_TICKS);
    }

    /** 残り 1 で効果を止める（エリトラと同じ。壊さない）。 */
    public static boolean usable(int damage, int maxDamage) {
        return damage < maxDamage - 1;
    }
}

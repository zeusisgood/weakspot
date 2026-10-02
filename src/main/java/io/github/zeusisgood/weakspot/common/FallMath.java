package io.github.zeusisgood.weakspot.common;

/**
 * 落下のダメージの見込み（1.11.0。落下の弱点の表示と、進捗「九死に一生」）。バニラの EntityLivingBase#fall と
 * applyPotionDamageCalculations と同じ式（落下は防具の防御値では減らないので、耐性のポーション効果と、防具の
 * エンチャントの保護値（落下耐性・ダメージ軽減）だけで減る）。
 */
public final class FallMath {

    /** これ以下の落下ではダメージを受けない（ブロック）。 */
    public static final double SAFE_DISTANCE = 3.0;
    /** エンチャントの保護値の上限（バニラと同じ）。 */
    private static final int MAX_PROTECTION = 20;

    /** 着地の見込み。 */
    public enum Outlook {
        /** ダメージなし。 */
        SAFE,
        /** ダメージはあるが死なない。 */
        HURT,
        /** 死ぬ見込み。 */
        LETHAL
    }

    private FallMath() {
    }

    /** 落ちた距離と跳躍力上昇のレベル（なければ 0）から、減らす前のダメージ（体力の点。ハート 1 つが 2 点）。 */
    public static int damagePoints(double fallDistance, int jumpBoostLevel) {
        return Math.max(0, (int) Math.ceil(fallDistance - SAFE_DISTANCE - jumpBoostLevel));
    }

    /**
     * 耐性のポーション効果のレベル（なければ 0）と、防具のエンチャントの保護値（EnchantmentHelper の値）で減らしたダメージ。
     */
    public static double damage(double fallDistance, int jumpBoostLevel, int resistanceLevel, int protection) {
        double damage = damagePoints(fallDistance, jumpBoostLevel);
        if (damage <= 0) {
            return 0;
        }
        if (resistanceLevel > 0) {
            damage = damage * Math.max(0, 25 - resistanceLevel * 5) / 25.0;
        }
        int epf = Math.max(0, Math.min(MAX_PROTECTION, protection));
        return damage * (1 - epf / 25.0);
    }

    /** 体力と衝撃吸収（黄色いハート）に対する見込み。不死のトーテムを持っていれば死なない。 */
    public static Outlook outlook(double damage, double health, double absorption, boolean totem) {
        if (damage <= 0) {
            return Outlook.SAFE;
        }
        return !totem && damage >= health + absorption ? Outlook.LETHAL : Outlook.HURT;
    }

    /** ハートの数の表示（2 点で 1。端数は多めに見る。例 4.5）。 */
    public static String heartsLabel(double damage) {
        int points = (int) Math.ceil(damage - 1e-9);
        return points % 2 == 0 ? Integer.toString(points / 2) : (points / 2) + ".5";
    }
}

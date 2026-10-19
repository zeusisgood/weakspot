package io.github.zeusisgood.weakspot.common;

/**
 * ヒットの間隔ごとに 1 回分戻り、BURST 回分まで貯められる。ラグでヒットの通知がまとめて届いても、BURST 回分までは落とさない。
 * 平均の速さの上限は「間隔ごとに 1 回」のまま。
 */
public final class HitBudget {

    public static final int BURST = 4;

    /** 最初は BURST 回分ある。 */
    private long next = Long.MIN_VALUE / 4;

    /** 記録はしない。間隔が 0 以下ならいつでも使える。 */
    public boolean ok(long now, int interval) {
        return interval <= 0 || next - (long) (BURST - 1) * interval <= now;
    }

    /** now は今より前の tick でもよい（掘る前のヒットの予約は、予約した tick で数える）。 */
    public void use(long now, int interval) {
        if (interval > 0) {
            next = Math.max(next, now) + interval;
        }
    }

    public boolean tryUse(long now, int interval) {
        if (!ok(now, interval)) {
            return false;
        }
        use(now, interval);
        return true;
    }
}

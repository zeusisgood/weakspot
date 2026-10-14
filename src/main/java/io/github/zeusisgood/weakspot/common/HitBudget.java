package io.github.zeusisgood.weakspot.common;

/**
 * ヒットの間隔（サーバー、全種類）。設定の間隔ごとに 1 回分戻り、BURST 回分まで貯められる。
 * ラグでヒットの通知がまとめて届いても、BURST 回分までは落とさない。平均の速さの上限は「間隔ごとに 1 回」のまま。
 *
 * 次に 1 回分が戻る時刻（next）だけを持つ。「next − (BURST − 1) × 間隔 ≤ 今」なら使える。
 */
public final class HitBudget {

    /** まとめて受け付ける回数（全種類で同じ）。 */
    public static final int BURST = 4;

    /** 最初は BURST 回分ある（十分に昔）。 */
    private long next = Long.MIN_VALUE / 4;

    /** 今使えるか（記録はしない）。間隔が 0 以下ならいつでも使える。 */
    public boolean ok(long now, int interval) {
        return interval <= 0 || next - (long) (BURST - 1) * interval <= now;
    }

    /** 1 回分使った。now は今より前の tick でもよい（掘る前のヒットの予約は、予約した tick で数える）。 */
    public void use(long now, int interval) {
        if (interval > 0) {
            next = Math.max(next, now) + interval;
        }
    }

    /** 使えれば使って true。 */
    public boolean tryUse(long now, int interval) {
        if (!ok(now, interval)) {
            return false;
        }
        use(now, interval);
        return true;
    }
}

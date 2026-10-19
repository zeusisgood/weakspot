package io.github.zeusisgood.weakspot.common;

/** 予約された tick で動くレッドストーンの部品の加速。 */
public final class ScheduledBoost {

    public static final int MAX_DISPENSES = 5;
    /** まとめて出すと音と煙が重なって連射に見えないので、分けて出す。 */
    public static final int BURST_INTERVAL_TICKS = 2;

    private ScheduledBoost() {
    }

    /** extra はこの tick に余分に進める tick。0 以下を返したら、今すぐ動かす。 */
    public static long advance(long remaining, int extra) {
        return remaining - Math.max(0, extra);
    }

    /** バニラの 1 回を含む。 */
    public static int dispenseCount(double multiplier, double baseMultiplier) {
        if (baseMultiplier <= 0) {
            return 1;
        }
        return (int) Math.max(1, Math.min(MAX_DISPENSES, Math.floor(1 + multiplier / baseMultiplier)));
    }
}

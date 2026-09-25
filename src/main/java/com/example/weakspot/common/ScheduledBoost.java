package com.example.weakspot.common;

/**
 * 予約された tick（スケジュール tick）で動くレッドストーンの部品の加速の計算（1.4.4）。
 * 信号の部品は予約の残り時間を前倒しし、ディスペンサー・ドロッパーは1回の予約で複数発にする。
 */
public final class ScheduledBoost {

    /** ディスペンサー・ドロッパーの、1回の予約での発射の上限。 */
    public static final int MAX_DISPENSES = 5;
    /** 余分の発射の間隔（tick）。まとめて出すと音と煙が重なって連射に見えないので、分けて出す（1.5.2）。 */
    public static final int BURST_INTERVAL_TICKS = 2;

    private ScheduledBoost() {
    }

    /**
     * 予約の新しい残り時間。remaining は今の残り（tick）、extra はこの tick に余分に進める tick。
     * 0 以下なら、今すぐ動かす。
     */
    public static long advance(long remaining, int extra) {
        return remaining - Math.max(0, extra);
    }

    /** ディスペンサー・ドロッパーの1回の予約での発射の回数（バニラの1回を含む）。 */
    public static int dispenseCount(double multiplier, double baseMultiplier) {
        if (baseMultiplier <= 0) {
            return 1;
        }
        return (int) Math.max(1, Math.min(MAX_DISPENSES, Math.floor(1 + multiplier / baseMultiplier)));
    }

    /** コンボの掛け数からの発射の回数（HUD の「発射 ×n」。サーバーの上限の設定は考えない）。 */
    public static int dispenseCountForFactor(double comboFactor) {
        return dispenseCount(comboFactor, 1);
    }
}

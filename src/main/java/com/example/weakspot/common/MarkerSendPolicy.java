package com.example.weakspot.common;

/**
 * 自分の弱点マークの状態をサーバーへ送るかどうか（クライアント側の送信頻度の上限）。
 * 変わったとき（出た・動いた・消えた）に送るが、前回から minInterval tick たつまでは待って、そのときの最新の状態を送る。
 * 出ている間は、受け手が時間切れで消さないように keepAliveTicks ごとに送り直す。
 */
public final class MarkerSendPolicy {

    private final int keepAliveTicks;
    private long lastSentTick = Long.MIN_VALUE / 2;

    public MarkerSendPolicy(int keepAliveTicks) {
        this.keepAliveTicks = keepAliveTicks;
    }

    /**
     * @param changed 前回送った状態から変わったか
     * @param present 今、弱点が出ているか
     * @return true なら今送る（送ったものとして記録する）
     */
    public boolean shouldSend(boolean changed, boolean present, long now, int minIntervalTicks) {
        boolean due = changed || (present && now - lastSentTick >= keepAliveTicks);
        if (!due || now - lastSentTick < minIntervalTicks) {
            return false;
        }
        lastSentTick = now;
        return true;
    }

    public void reset() {
        lastSentTick = Long.MIN_VALUE / 2;
    }
}

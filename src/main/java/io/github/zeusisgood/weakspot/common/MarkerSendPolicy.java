package io.github.zeusisgood.weakspot.common;

/**
 * 自分の弱点マークの状態をサーバーへ送る頻度の上限（クライアント側）。変わっても、前回から minInterval tick たつまでは
 * 待って最新の状態を送る。出ている間は、受け手が時間切れで消さないように keepAliveTicks ごとに送り直す。
 */
public final class MarkerSendPolicy {

    private final int keepAliveTicks;
    private long lastSentTick = Long.MIN_VALUE / 2;

    public MarkerSendPolicy(int keepAliveTicks) {
        this.keepAliveTicks = keepAliveTicks;
    }

    /** true を返したら、送ったものとして記録する。 */
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

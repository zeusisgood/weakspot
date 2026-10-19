package io.github.zeusisgood.weakspot.common;

/**
 * クライアントとサーバーのコンボを揃える。サーバーの数が正で、ずれたらサーバーが本人に今の数を送り、
 * クライアントは表示を直す（片道だけ。サーバーはクライアントの数に合わせない）。
 * <p>
 * 当て続けている間は送らない・直さない。届く前に次のヒットを送っていると、直すと 1 つ足りなくなるため。
 */
public final class ComboSync {

    public static final int IDLE_TICKS = 3;
    /** でたらめな数を付けたヒットを大量に送られても、送信が増えないように。 */
    public static final int MIN_SEND_INTERVAL_TICKS = 4;

    private ComboSync() {
    }

    /** 掘る前の採掘ヒットの予約があれば、クライアントはもう数えていて、サーバーは掘り始めたら数えるので 1 を足す。 */
    public static int expected(int serverCount, boolean reserved) {
        return serverCount + (reserved ? 1 : 0);
    }

    public static boolean mismatched(int clientStreak, int serverCount, boolean reserved) {
        return clientStreak != expected(serverCount, reserved);
    }

    public static boolean canSend(long now, long lastHitTick, long lastSentTick) {
        return now - lastHitTick >= IDLE_TICKS && now - lastSentTick >= MIN_SEND_INTERVAL_TICKS;
    }

    public static boolean canCorrect(long now, long lastHitTick, int currentCount) {
        return currentCount > 0 && now - lastHitTick >= IDLE_TICKS;
    }
}

package io.github.zeusisgood.weakspot.common;

/**
 * クライアントとサーバーのコンボを揃える（1.11.3）。サーバーの数が正で、ずれたらサーバーが本人に今の数を送り、
 * クライアントは表示を直す（片道だけ。サーバーはクライアントの数に合わせない）。
 * <p>
 * 当て続けている間は送らない・直さない。届く前に次のヒットを送っていると、直すと 1 つ足りなくなるため。
 */
public final class ComboSync {

    /** 最後のヒットからこの tick たつまで、サーバーは送らず、クライアントは直さない。 */
    public static final int IDLE_TICKS = 3;
    /** 本人宛ての送信の最短の間隔（tick）。でたらめな数を付けたヒットを大量に送られても、送信が増えないように。 */
    public static final int MIN_SEND_INTERVAL_TICKS = 4;

    private ComboSync() {
    }

    /**
     * サーバーから見た、そのヒットのあとのコンボ数。掘る前の採掘ヒットの予約があれば、クライアントはもう数えていて、
     * サーバーは掘り始めたら数えるので 1 を足す。
     */
    public static int expected(int serverCount, boolean reserved) {
        return serverCount + (reserved ? 1 : 0);
    }

    /** クライアントが送ってきたヒット後の数が、サーバーの数とずれているか。 */
    public static boolean mismatched(int clientStreak, int serverCount, boolean reserved) {
        return clientStreak != expected(serverCount, reserved);
    }

    /** サーバーが本人に今の数を送ってよいか（最後のヒットから IDLE_TICKS たち、前に送ってから間があいている）。 */
    public static boolean canSend(long now, long lastHitTick, long lastSentTick) {
        return now - lastHitTick >= IDLE_TICKS && now - lastSentTick >= MIN_SEND_INTERVAL_TICKS;
    }

    /** クライアントが届いた数で直してよいか（最後のヒットから IDLE_TICKS たち、まだ途切れていない）。 */
    public static boolean canCorrect(long now, long lastHitTick, int currentCount) {
        return currentCount > 0 && now - lastHitTick >= IDLE_TICKS;
    }
}

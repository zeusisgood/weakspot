package io.github.zeusisgood.weakspot.common;

/** 連続ヒット数（コンボ）。すべての種類とブロックをまたいで続き、数は戻らずに上がり続ける。 */
public final class HitStreak {

    /** ちょうどこの tick あいたヒットは続く。 */
    public static final int RESET_TICKS = 40;

    private int count;
    private long lastHitTick;
    private int resetTicks = RESET_TICKS;

    /** ヒット後の数を返す。 */
    public int hit(long now) {
        if (count > 0 && isExpired(now)) {
            count = 0;
        }
        count++;
        lastHitTick = now;
        return count;
    }

    /** 途切れる前の数を返す（途切れていなければ 0）。毎 tick 呼べば、途切れた瞬間が分かる。 */
    public int expire(long now) {
        if (count == 0 || !isExpired(now)) {
            return 0;
        }
        int broken = count;
        count = 0;
        return broken;
    }

    public int count(long now) {
        return count > 0 && !isExpired(now) ? count : 0;
    }

    /** 数が 0 のときは意味がない。 */
    public long lastHitTick() {
        return lastHitTick;
    }

    /** 最後のヒットの tick は変えない（途切れるまでの残り時間はそのまま）。0 以下で 0 に戻したときは、expire は途切れた数を返さない。 */
    public void correct(int newCount) {
        count = Math.max(0, newCount);
    }

    public void reset() {
        count = 0;
    }

    /** 途切れるまでの tick（コンボの猶予の効果で延びる）。持ち主が効果の有無を見て、数える・確かめる前に入れる。 */
    public void setResetTicks(int ticks) {
        resetTicks = ticks;
    }

    public int resetTicks() {
        return resetTicks;
    }

    private boolean isExpired(long now) {
        return now - lastHitTick > resetTicks;
    }

    /** 1（ヒットの直後）〜0。 */
    public static double remainingFraction(double ticksSinceHit, int resetTicks) {
        return Math.max(0, Math.min(1, 1 - ticksSinceHit / resetTicks));
    }
}

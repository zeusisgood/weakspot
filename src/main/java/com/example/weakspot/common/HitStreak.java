package com.example.weakspot.common;

/**
 * 連続ヒット数（コンボ）。種類（採掘・成長・機械）やブロックをまたいで続き、RESET_TICKS より長くヒットがないと 0 に戻る。
 * 数は戻らずに上がり続ける。ヒット音の音階は、この数から求める（HitPitch。1オクターブで最初の音に戻る）。
 */
public final class HitStreak {

    /** この tick を超えてヒットがなければ途切れる（ちょうどこの tick あいたヒットは続く）。 */
    public static final int RESET_TICKS = 40;

    private int count;
    private long lastHitTick;

    /** ヒットした。途切れていれば 1 から数え直す。ヒット後の数を返す。 */
    public int hit(long now) {
        if (count > 0 && isExpired(now)) {
            count = 0;
        }
        count++;
        lastHitTick = now;
        return count;
    }

    /**
     * 途切れていれば 0 に戻し、途切れる前の数を返す（途切れていなければ 0）。毎 tick 呼べば、途切れた瞬間が分かる。
     */
    public int expire(long now) {
        if (count == 0 || !isExpired(now)) {
            return 0;
        }
        int broken = count;
        count = 0;
        return broken;
    }

    /** 今の数（途切れていれば 0）。 */
    public int count(long now) {
        return count > 0 && !isExpired(now) ? count : 0;
    }

    /** 最後のヒットの tick（数が 0 のときは意味がない）。 */
    public long lastHitTick() {
        return lastHitTick;
    }

    /** ワールドを出た、死亡した、ディメンションを移動したときなど。 */
    public void reset() {
        count = 0;
    }

    private boolean isExpired(long now) {
        return now - lastHitTick > RESET_TICKS;
    }

    /** 途切れるまでの残り時間の割合（最後のヒットの直後が 1.0、RESET_TICKS たつと 0.0）。 */
    public static double remainingFraction(double ticksSinceHit) {
        return Math.max(0, Math.min(1, 1 - ticksSinceHit / RESET_TICKS));
    }
}

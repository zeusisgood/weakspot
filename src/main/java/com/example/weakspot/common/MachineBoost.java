package com.example.weakspot.common;

/**
 * 1つの機械の加速の状態。記録がある間、毎tick、update() を (倍率 − 1) 回余分に呼ぶ。
 * 倍率が小数のときは端数を持ち越して、平均で (倍率 − 1) 回になるようにする。
 * 複数のヒット（複数のプレイヤーを含む）が重なっても足さず、大きいほうだけを使う。
 */
public final class MachineBoost {

    private double multiplier = 1;
    private int remainingTicks;
    private double carry;

    /** ヒットを受け付けたときに呼ぶ。 */
    public void hit(double multiplier, int durationTicks) {
        this.multiplier = isActive() ? Math.max(this.multiplier, multiplier) : multiplier;
        this.remainingTicks = Math.max(remainingTicks, durationTicks);
    }

    public boolean isActive() {
        return remainingTicks > 0;
    }

    /** この tick に余分に呼ぶ回数を返し、残り時間を1減らす。 */
    public int nextTickCalls() {
        if (!isActive()) {
            return 0;
        }
        remainingTicks--;
        double extra = Math.max(0, multiplier - 1) + carry;
        int calls = (int) Math.floor(extra);
        carry = extra - calls;
        if (!isActive()) {
            carry = 0;
        }
        return calls;
    }
}

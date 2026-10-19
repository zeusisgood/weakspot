package io.github.zeusisgood.weakspot.common;

/**
 * 1 つの機械の加速。毎 tick、update() を (倍率 − 1) 回余分に呼ぶ（端数は持ち越す）。
 * 複数のヒット（複数のプレイヤーを含む）が重なっても足さず、大きいほうだけを使う。
 */
public final class MachineBoost {

    private double multiplier = 1;
    private int remainingTicks;
    private double carry;
    private double particleCarry;

    public void hit(double multiplier, int durationTicks) {
        this.multiplier = isActive() ? Math.max(this.multiplier, multiplier) : multiplier;
        this.remainingTicks = Math.max(remainingTicks, durationTicks);
    }

    public double multiplier() {
        return multiplier;
    }

    /** 残り時間は減らさない。 */
    public int nextParticles() {
        if (!isActive()) {
            return 0;
        }
        double count = multiplier / MachineParticles.PARTICLES_DIVISOR + particleCarry;
        int whole = (int) Math.floor(count);
        particleCarry = count - whole;
        return whole;
    }

    public boolean isActive() {
        return remainingTicks > 0;
    }

    /** 残り時間を 1 減らす。 */
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

package io.github.zeusisgood.weakspot.common;

/** 自分の側で覚える、時間で続く加速。時間は呼ぶ側の tick（クライアントは一時停止中に止まる tick）。 */
public final class TimedBoost {

    private double multiplier = 1;
    private long until = Long.MIN_VALUE / 2;
    private int duration = 1;

    public void start(double newMultiplier, int ticks, long now) {
        multiplier = newMultiplier;
        duration = Math.max(1, ticks);
        until = now + duration;
    }

    public void clear() {
        until = Long.MIN_VALUE / 2;
    }

    public boolean isActive(long now) {
        return now < until;
    }

    public double multiplier() {
        return multiplier;
    }

    public double remainingFraction(long now, float partialTicks) {
        return Math.max(0, Math.min(1, (until - now - partialTicks) / duration));
    }
}

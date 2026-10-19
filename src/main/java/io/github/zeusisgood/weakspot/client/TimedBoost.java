package io.github.zeusisgood.weakspot.client;

/** 時間は ClientWeakSpotHandler.clientTick（一時停止中は止まる）。 */
final class TimedBoost {

    private double multiplier = 1;
    private long until = Long.MIN_VALUE / 2;
    private int duration = 1;

    void start(double newMultiplier, int ticks, long now) {
        multiplier = newMultiplier;
        duration = Math.max(1, ticks);
        until = now + duration;
    }

    void clear() {
        until = Long.MIN_VALUE / 2;
    }

    boolean isActive(long now) {
        return now < until;
    }

    double multiplier() {
        return multiplier;
    }

    double remainingFraction(long now, float partialTicks) {
        return Math.max(0, Math.min(1, (until - now - partialTicks) / duration));
    }
}

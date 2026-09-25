package com.example.weakspot.client;

/**
 * 自分の側で覚えている、時間で続く加速（1.7.0。はしご・走り。乗り物の VehicleSpot と同じ考え方）。
 * 時間は ClientWeakSpotHandler.clientTick で数える（一時停止中は止まる）。
 */
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

    /** 残り時間の割合（0〜1）。partialTicks でなめらかにする。 */
    double remaining(long now, float partialTicks) {
        return Math.max(0, Math.min(1, (until - now - partialTicks) / duration));
    }
}

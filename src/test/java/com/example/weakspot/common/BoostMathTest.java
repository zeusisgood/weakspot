package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BoostMathTest {

    @Test
    public void defaultHitGivesTwelveExtraTicks() {
        // 4倍 × 4tick = 通常の約0.6秒 (12tick) 分の追加進捗
        assertEquals(12, BoostMath.extraTicksPerHit(4.0, 4), 1e-9);
    }

    @Test
    public void serverFactorMatchesAccumulatedProgress() {
        // 経過 i tick のとき、サーバーは speed × (i + 1) で判定する
        double speed = 0.05;
        long elapsed = 9;
        double extra = 12;
        double integrated = speed * (elapsed + 1 + extra);
        double judged = speed * BoostMath.serverSpeedFactor(extra, elapsed) * (elapsed + 1);
        assertEquals(integrated, judged, 1e-9);
    }

    @Test
    public void noExtraMeansNoChange() {
        assertEquals(1, BoostMath.serverSpeedFactor(0, 10), 1e-9);
    }
}

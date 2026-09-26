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

    @Test
    public void comboFactorScalesExtraTicks() {
        assertEquals(12, BoostMath.extraTicksPerHit(4.0, 4, 1.0), 1e-9);
        assertEquals(24, BoostMath.extraTicksPerHit(4.0, 4, 2.0), 1e-9);
        assertEquals(15, BoostMath.extraTicksPerHit(4.0, 4, 1.25), 1e-9);
    }

    @Test
    public void clientMultiplierMatchesServerExtra() {
        // 時間枠 d tick の間 (1 + (m − 1)f) 倍なら、上乗せは (m − 1) f d tick で、サーバーの追加進捗と同じ
        double m = 4.0;
        int d = 4;
        for (double f : new double[] {1.0, 1.25, 2.0, 4.0}) {
            assertEquals(BoostMath.extraTicksPerHit(m, d, f), (BoostMath.clientMultiplier(m, f) - 1) * d, 1e-9);
        }
        assertEquals(4.0, BoostMath.clientMultiplier(4.0, 1.0), 1e-9);
    }
}

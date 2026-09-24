package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public class FishingMathTest {

    @Test
    public void sixHitsMakeOneHundredTicksEach() {
        assertEquals(100, FishingMath.ticksPerHit(6));
        assertEquals(0, FishingMath.ticksPerHit(0));
        assertEquals(0, FishingMath.ticksPerHit(-1));
    }

    @Test
    public void waitNeverGoesBelowZero() {
        assertEquals(300, FishingMath.waitAfterHit(400, 100));
        assertEquals(1, FishingMath.waitAfterHit(50, 100));
        assertTrue(FishingMath.waitAfterHit(0, 100) >= 0);
    }

    @Test
    public void progressIsBetweenZeroAndOne() {
        assertEquals(0.0, FishingMath.progress(400, 400), 1e-9);
        assertEquals(0.75, FishingMath.progress(400, 100), 1e-9);
        assertEquals(1.0, FishingMath.progress(400, -10), 1e-9);
        assertEquals(0.0, FishingMath.progress(400, 500), 1e-9);
        assertEquals(0.0, FishingMath.progress(0, 0), 1e-9);
    }

    @Test
    public void nextSpotStaysNearTheBobberAndMovesAway() {
        Random random = new Random(7);
        double dx = 0;
        double dz = 0;
        for (int i = 0; i < 2000; i++) {
            double[] next = FishingMath.nextSpot(dx, dz, random);
            assertTrue(Math.hypot(next[0], next[1]) <= FishingMath.SPOT_RADIUS + 1e-9);
            assertTrue(Math.hypot(next[0] - dx, next[1] - dz) >= FishingMath.MIN_MOVE - 1e-9);
            dx = next[0];
            dz = next[1];
        }
    }

    @Test
    public void allowedAngleIsTheBoundaryForTheCircleRadius() {
        double fov = 70;
        double height = 1080;
        double radius = 24;
        double allowed = FishingMath.allowedAngle(radius, fov, height);
        // 画面の中央から radius ちょうど上の点の、視線との角度
        double focal = (height / 2) / Math.tan(Math.toRadians(fov) / 2);
        double angle = Math.atan(radius / focal);
        assertEquals(angle, allowed, 1e-12);
        assertTrue(FishingMath.isAimed(angle * 0.99, allowed));
        assertFalse(FishingMath.isAimed(angle * 1.01, allowed));
    }

    @Test
    public void allowedAngleGrowsWithRadiusAndFov() {
        assertTrue(FishingMath.allowedAngle(24, 70, 1080) > FishingMath.allowedAngle(12, 70, 1080));
        assertTrue(FishingMath.allowedAngle(24, 110, 1080) > FishingMath.allowedAngle(24, 70, 1080));
        assertTrue(FishingMath.allowedAngle(24, 70, 540) > FishingMath.allowedAngle(24, 70, 1080));
    }
}

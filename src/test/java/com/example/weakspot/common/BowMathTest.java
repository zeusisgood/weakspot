package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public class BowMathTest {

    @Test
    public void vectorAndRotationAreInverse() {
        double[][] cases = {{0, 0}, {90, 0}, {-135, 30}, {170, -60}, {45, 89}};
        for (double[] c : cases) {
            double[] r = BowMath.rotation(BowMath.vector(c[0], c[1]));
            assertEquals(0, BowMath.wrapDegrees(r[0] - c[0]), 1e-6);
            assertEquals(c[1], r[1], 1e-6);
        }
    }

    @Test
    public void vectorMatchesMinecraftDirections() {
        // yaw 0 は +Z（南）、yaw 90 は -X（西）、pitch 90 は真下
        double[] south = BowMath.vector(0, 0);
        assertEquals(1, south[2], 1e-9);
        double[] west = BowMath.vector(90, 0);
        assertEquals(-1, west[0], 1e-9);
        double[] down = BowMath.vector(0, 90);
        assertEquals(-1, down[1], 1e-9);
    }

    @Test
    public void nextSpotIsNearTheLookAndMovesAway() {
        Random random = new Random(3);
        double[][] looks = {{0, 0}, {179, 10}, {-179, -40}, {90, 80}};
        for (double[] look : looks) {
            double prevYaw = look[0];
            double prevPitch = look[1];
            for (int i = 0; i < 500; i++) {
                double[] next = BowMath.nextSpot(look[0], look[1], prevYaw, prevPitch, random);
                double offset = BowMath.angleBetween(look[0], look[1], next[0], next[1]);
                assertTrue("offset " + offset, offset >= BowMath.MIN_OFFSET_DEGREES - 1e-6);
                assertTrue("offset " + offset, offset <= BowMath.MAX_OFFSET_DEGREES + 1e-6);
                assertTrue(BowMath.angleBetween(prevYaw, prevPitch, next[0], next[1])
                        >= BowMath.MIN_MOVE_DEGREES - 1e-6);
                // 前の向きから遠回りしない
                assertTrue(Math.abs(next[0] - prevYaw) <= 180);
                prevYaw = next[0];
                prevPitch = next[1];
            }
        }
    }

    @Test
    public void hitNeverDrawsPastFull() {
        assertEquals(5, BowMath.addedTicks(0, 5));
        assertEquals(5, BowMath.addedTicks(15, 5));
        assertEquals(2, BowMath.addedTicks(18, 5));
        assertEquals(0, BowMath.addedTicks(20, 5));
        assertEquals(0, BowMath.addedTicks(40, 5));
        assertEquals(0, BowMath.addedTicks(5, 0));
        assertFalse(BowMath.isFull(19));
        assertTrue(BowMath.isFull(20));
    }

    @Test
    public void barValueStaysInRange() {
        assertEquals(0.0, BowMath.barValue(0), 1e-9);
        assertEquals(0.5, BowMath.barValue(10), 1e-9);
        assertEquals(1.0, BowMath.barValue(20), 1e-9);
        assertEquals(1.0, BowMath.barValue(35.5), 1e-9);
        assertEquals(0.0, BowMath.barValue(-1), 1e-9);
    }

    @Test
    public void wrapDegreesKeepsTheRange() {
        assertEquals(-170, BowMath.wrapDegrees(190), 1e-9);
        assertEquals(170, BowMath.wrapDegrees(-190), 1e-9);
        assertEquals(0, BowMath.wrapDegrees(720), 1e-9);
    }

    @Test
    public void overchargeAddsTenPercentUpToFiftyPercent() {
        assertEquals(1.0, BowMath.overchargeMultiplier(0), 1e-9);
        assertEquals(1.3, BowMath.overchargeMultiplier(3), 1e-9);
        assertEquals(1.5, BowMath.overchargeMultiplier(5), 1e-9);
        assertEquals(1.5, BowMath.overchargeMultiplier(9), 1e-9);
        assertTrue(BowMath.canOvercharge(4));
        assertFalse(BowMath.canOvercharge(5));
    }

    @Test
    public void offsetRangeIsCappedByTheFieldOfView() {
        double[] wide = BowMath.offsetRange(70);
        assertEquals(10.0, wide[0], 1e-9);
        assertEquals(20.0, wide[1], 1e-9);
        double[] narrow = BowMath.offsetRange(30);
        assertEquals(10.5, narrow[1], 1e-9);
        assertEquals(5.25, narrow[0], 1e-9);
        double[] unknown = BowMath.offsetRange(0);
        assertEquals(20.0, unknown[1], 1e-9);
    }

    @Test
    public void nextSpotStaysWithinTheNarrowRange() {
        java.util.Random random = new java.util.Random(7);
        for (int i = 0; i < 200; i++) {
            double[] next = BowMath.nextSpot(30, -10, 30, -10, 30, random);
            double offset = BowMath.angleBetween(30, -10, next[0], next[1]);
            assertTrue("offset " + offset, offset <= 10.5 + 1e-6);
            assertTrue("offset " + offset, offset >= 5.25 - 1e-6);
        }
    }
}

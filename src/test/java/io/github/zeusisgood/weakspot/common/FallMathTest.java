package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FallMathTest {

    @Test
    public void noDamageUpToThreeBlocks() {
        assertEquals(0, FallMath.damagePoints(0, 0));
        assertEquals(0, FallMath.damagePoints(3.0, 0));
        assertEquals(1, FallMath.damagePoints(3.2, 0));
        assertEquals(17, FallMath.damagePoints(20, 0));
    }

    @Test
    public void jumpBoostSoftensTheFall() {
        assertEquals(15, FallMath.damagePoints(20, 2));
        assertEquals(0, FallMath.damagePoints(4, 1));
    }

    @Test
    public void heartsAreHalfThePoints() {
        assertEquals("0", FallMath.heartsLabel(0));
        assertEquals("0.5", FallMath.heartsLabel(1));
        assertEquals("8.5", FallMath.heartsLabel(17));
        assertEquals("10", FallMath.heartsLabel(20));
    }
}

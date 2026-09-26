package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimedBoostMathTest {

    @Test
    public void comboStacksWithoutCapByDefault() {
        assertEquals(1.5, TimedBoostMath.multiplier(1.5, 0, 1), 1e-9);
        assertEquals(3.0, TimedBoostMath.multiplier(1.5, 0, 100), 1e-9);
        assertEquals(6.0, TimedBoostMath.multiplier(1.5, 0, 1000), 1e-9);
    }

    @Test
    public void capWhenSet() {
        assertEquals(3.0, TimedBoostMath.multiplier(1.5, 3.0, 1000), 1e-9);
        assertEquals(1.5, TimedBoostMath.multiplier(1.5, 3.0, 1), 1e-9);
    }

    @Test
    public void extraIsNeverNegative() {
        assertEquals(0.5, TimedBoostMath.extra(1.5), 1e-9);
        assertEquals(0, TimedBoostMath.extra(0.5), 1e-9);
    }
}

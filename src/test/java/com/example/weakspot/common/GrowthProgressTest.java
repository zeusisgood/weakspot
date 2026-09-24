package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GrowthProgressTest {

    @Test
    public void fractionOfAge() {
        assertEquals(0.0, GrowthProgress.fraction(0, 7), 1e-9);
        assertEquals(1.0, GrowthProgress.fraction(7, 7), 1e-9);
        assertEquals(3.0 / 7, GrowthProgress.fraction(3, 7), 1e-9);
    }

    @Test
    public void clampsOutOfRange() {
        assertEquals(0.0, GrowthProgress.fraction(-2, 7), 1e-9);
        assertEquals(1.0, GrowthProgress.fraction(9, 7), 1e-9);
        assertEquals(0.0, GrowthProgress.fraction(3, 0), 1e-9);
    }

    @Test
    public void columnUsesAgeOutOfFifteen() {
        assertEquals(0.0, GrowthProgress.column(0), 1e-9);
        assertEquals(7.0 / 15, GrowthProgress.column(7), 1e-9);
        assertEquals(1.0, GrowthProgress.column(15), 1e-9);
        assertEquals(1.0, GrowthProgress.column(20), 1e-9);
    }
}

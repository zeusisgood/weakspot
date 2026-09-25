package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VehicleBoostMathTest {

    @Test
    public void comboStacksWithoutCapByDefault() {
        assertEquals(1.5, VehicleBoostMath.multiplier(1.5, 0, 1), 1e-9);
        assertEquals(3.0, VehicleBoostMath.multiplier(1.5, 0, 100), 1e-9);
        assertEquals(6.0, VehicleBoostMath.multiplier(1.5, 0, 1000), 1e-9);
    }

    @Test
    public void capWhenSet() {
        assertEquals(3.0, VehicleBoostMath.multiplier(1.5, 3.0, 1000), 1e-9);
        assertEquals(1.5, VehicleBoostMath.multiplier(1.5, 3.0, 1), 1e-9);
    }

    @Test
    public void extraIsNeverNegative() {
        assertEquals(0.5, VehicleBoostMath.extra(1.5), 1e-9);
        assertEquals(0, VehicleBoostMath.extra(0.5), 1e-9);
    }
}

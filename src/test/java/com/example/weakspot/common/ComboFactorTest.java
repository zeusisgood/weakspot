package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ComboFactorTest {

    @Test
    public void factorRisesAtTheSteps() {
        assertEquals(1.0, ComboFactor.factor(0), 0);
        assertEquals(1.0, ComboFactor.factor(24), 0);
        assertEquals(1.25, ComboFactor.factor(25), 0);
        assertEquals(1.5, ComboFactor.factor(50), 0);
        assertEquals(1.5, ComboFactor.factor(99), 0);
        assertEquals(2.0, ComboFactor.factor(100), 0);
        assertEquals(2.5, ComboFactor.factor(250), 0);
        assertEquals(3.0, ComboFactor.factor(500), 0);
        assertEquals(4.0, ComboFactor.factor(1000), 0);
        assertEquals(4.0, ComboFactor.factor(5000), 0);
    }
}

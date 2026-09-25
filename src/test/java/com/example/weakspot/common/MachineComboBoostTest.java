package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MachineComboBoostTest {

    @Test
    public void factorRisesAtTheSteps() {
        assertEquals(1.0, MachineComboBoost.factor(0), 0);
        assertEquals(1.0, MachineComboBoost.factor(24), 0);
        assertEquals(1.25, MachineComboBoost.factor(25), 0);
        assertEquals(1.5, MachineComboBoost.factor(50), 0);
        assertEquals(1.5, MachineComboBoost.factor(99), 0);
        assertEquals(2.0, MachineComboBoost.factor(100), 0);
        assertEquals(2.5, MachineComboBoost.factor(250), 0);
        assertEquals(3.0, MachineComboBoost.factor(500), 0);
        assertEquals(4.0, MachineComboBoost.factor(1000), 0);
        assertEquals(4.0, MachineComboBoost.factor(5000), 0);
    }

    @Test
    public void defaultsReachSixteenAtAThousand() {
        assertEquals(4.0, MachineComboBoost.multiplier(4.0, 16.0, 1), 0);
        assertEquals(10.0, MachineComboBoost.multiplier(4.0, 16.0, 250), 0);
        assertEquals(16.0, MachineComboBoost.multiplier(4.0, 16.0, 1000), 0);
    }

    @Test
    public void capWins() {
        assertEquals(8.0, MachineComboBoost.multiplier(4.0, 8.0, 1000), 0);
        // 上限が base より小さくても、上限を優先する
        assertEquals(2.0, MachineComboBoost.multiplier(4.0, 2.0, 1), 0);
    }

    @Test
    public void speedLabels() {
        assertEquals("4", MachineComboBoost.speedLabel(4.0));
        assertEquals("16", MachineComboBoost.speedLabel(16.0));
        assertEquals("7.5", MachineComboBoost.speedLabel(7.5));
        assertEquals("6.3", MachineComboBoost.speedLabel(6.25));
    }

    @Test
    public void labels() {
        assertEquals("1.25", MachineComboBoost.label(1.25));
        assertEquals("2.5", MachineComboBoost.label(2.5));
        assertEquals("2.0", MachineComboBoost.label(2.0));
    }
}

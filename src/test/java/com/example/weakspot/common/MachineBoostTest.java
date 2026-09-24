package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineBoostTest {

    private static int totalCalls(MachineBoost boost) {
        int total = 0;
        while (boost.isActive()) {
            total += boost.nextTickCalls();
        }
        return total;
    }

    @Test
    public void defaultHitGivesTwelveExtraUpdates() {
        // 4倍 × 4tick = 毎tick 3回 × 4tick
        MachineBoost boost = new MachineBoost();
        boost.hit(4.0, 4);
        assertEquals(3, boost.nextTickCalls());
        assertEquals(9, totalCalls(boost));
        assertFalse(boost.isActive());
    }

    @Test
    public void fractionalMultiplierCarriesOver() {
        MachineBoost boost = new MachineBoost();
        boost.hit(1.5, 4);
        assertEquals(2, totalCalls(boost));
    }

    @Test
    public void overlappingHitsDoNotStack() {
        MachineBoost boost = new MachineBoost();
        boost.hit(4.0, 4);
        boost.nextTickCalls();
        boost.hit(4.0, 4);
        // 残り3tickが4tickに延びるだけで、倍率は足さない
        assertEquals(12, totalCalls(boost));
    }

    @Test
    public void largerMultiplierWins() {
        MachineBoost boost = new MachineBoost();
        boost.hit(2.0, 4);
        boost.hit(4.0, 2);
        assertEquals(3, boost.nextTickCalls());
        assertTrue(boost.isActive());
    }

    @Test
    public void inactiveBoostCallsNothing() {
        MachineBoost boost = new MachineBoost();
        assertFalse(boost.isActive());
        assertEquals(0, boost.nextTickCalls());
    }
}

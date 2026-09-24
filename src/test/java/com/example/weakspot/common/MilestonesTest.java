package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class MilestonesTest {

    private static final int[] DEFAULT = {100, 777, 1000, 10000};

    @Test
    public void repairsEveryFifthHit() {
        assertFalse(Milestones.isRepairHit(4, 5));
        assertTrue(Milestones.isRepairHit(5, 5));
        assertFalse(Milestones.isRepairHit(6, 5));
        assertTrue(Milestones.isRepairHit(10, 5));
    }

    @Test
    public void repairDisabledByZeroInterval() {
        assertFalse(Milestones.isRepairHit(5, 0));
        assertFalse(Milestones.isRepairHit(0, 5));
    }

    @Test
    public void reachesMilestoneOnlyAtExactCount() {
        assertEquals(Collections.emptyList(), Milestones.reached(DEFAULT, 99));
        assertEquals(Collections.singletonList(0), Milestones.reached(DEFAULT, 100));
        assertEquals(Collections.emptyList(), Milestones.reached(DEFAULT, 101));
        assertEquals(Collections.singletonList(1), Milestones.reached(DEFAULT, 777));
        assertEquals(Collections.singletonList(3), Milestones.reached(DEFAULT, 10000));
    }

    @Test
    public void duplicateMilestonesAllFire() {
        assertEquals(Arrays.asList(0, 2), Milestones.reached(new int[] {50, 60, 50}, 50));
    }

    @Test
    public void missingAmountsAreZero() {
        int[] xp = {10, 77};
        assertEquals(10, Milestones.amountAt(xp, 0));
        assertEquals(77, Milestones.amountAt(xp, 1));
        assertEquals(0, Milestones.amountAt(xp, 2));
        assertEquals(0, Milestones.amountAt(new int[] {-5}, 0));
    }

    @Test
    public void repairDoesNotGoBelowZero() {
        assertEquals(9, Milestones.repairedDamage(10, 1));
        assertEquals(0, Milestones.repairedDamage(3, 77));
    }
}

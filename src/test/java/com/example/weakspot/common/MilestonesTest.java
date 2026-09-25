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

    @Test
    public void repeatsAfterTheLargestMilestone() {
        int[] milestones = {100, 777, 1000};
        assertEquals(1, Milestones.reachedWithRepeat(milestones, 500, 1000).size());
        assertEquals(1000L, Milestones.reachedWithRepeat(milestones, 500, 1000).get(0)[0]);
        assertEquals(2L, Milestones.reachedWithRepeat(milestones, 500, 1000).get(0)[1]);
        // 1000 より先は 500 ごと。ごほうびは最後の添字
        assertEquals(1500L, Milestones.reachedWithRepeat(milestones, 500, 1500).get(0)[0]);
        assertEquals(2L, Milestones.reachedWithRepeat(milestones, 500, 1500).get(0)[1]);
        assertEquals(1, Milestones.reachedWithRepeat(milestones, 500, 2000).size());
        assertTrue(Milestones.reachedWithRepeat(milestones, 500, 1499).isEmpty());
        assertTrue(Milestones.reachedWithRepeat(milestones, 0, 1500).isEmpty());
        assertTrue(Milestones.reachedWithRepeat(new int[0], 500, 500).isEmpty());
    }

    @Test
    public void onlySevensAreLucky() {
        assertTrue(Milestones.isLucky(777));
        assertTrue(Milestones.isLucky(7777));
        assertTrue(Milestones.isLucky(777777));
        assertFalse(Milestones.isLucky(770));
        assertFalse(Milestones.isLucky(1000));
        assertFalse(Milestones.isLucky(0));
    }
}

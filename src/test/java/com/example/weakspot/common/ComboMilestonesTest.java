package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ComboMilestonesTest {

    @Test
    public void trueOnlyAtTheSteps() {
        ComboMilestones milestones = new ComboMilestones();
        List<Integer> fired = new ArrayList<>();
        for (int combo = 1; combo <= 3500; combo++) {
            if (milestones.reached(combo)) {
                fired.add(combo);
            }
        }
        assertEquals(Arrays.asList(10, 25, 50, 100, 250, 500, 1000, 2000, 3000), fired);
    }

    @Test
    public void glowColorsFrom250() {
        assertEquals(-1, ComboMilestones.glowRgb(100));
        assertEquals(0xFF55FF, ComboMilestones.glowRgb(250));
        assertEquals(0x55FFFF, ComboMilestones.glowRgb(500));
        assertEquals(0xFFD700, ComboMilestones.glowRgb(1000));
        assertEquals(0xFFD700, ComboMilestones.glowRgb(3000));
    }

    @Test
    public void sameNumberTwiceFiresOnce() {
        ComboMilestones milestones = new ComboMilestones();
        assertTrue(milestones.reached(10));
        assertFalse(milestones.reached(10));
        assertFalse(milestones.reached(10));
    }

    @Test
    public void firesAgainInANewCombo() {
        ComboMilestones milestones = new ComboMilestones();
        assertTrue(milestones.reached(10));
        milestones.reached(1);
        assertTrue(milestones.reached(10));
        milestones.reset();
        assertTrue(milestones.reached(10));
    }
}

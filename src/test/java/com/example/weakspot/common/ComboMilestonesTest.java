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
        for (int combo = 1; combo <= 120; combo++) {
            if (milestones.reached(combo)) {
                fired.add(combo);
            }
        }
        assertEquals(Arrays.asList(10, 25, 50, 100), fired);
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

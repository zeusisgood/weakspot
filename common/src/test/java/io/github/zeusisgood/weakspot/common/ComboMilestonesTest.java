package io.github.zeusisgood.weakspot.common;

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
        for (int combo = 1; combo <= 650; combo++) {
            if (milestones.reached(combo)) {
                fired.add(combo);
            }
        }
        assertEquals(Arrays.asList(10, 25, 50, 75, 100, 150, 200, 250, 300, 400, 500, 600), fired);
    }

    @Test
    public void glowColorsFrom150() {
        assertEquals(-1, ComboMilestones.glowRgb(100));
        assertEquals(0x5599FF, ComboMilestones.glowRgb(150));
        assertEquals(0x55FFFF, ComboMilestones.glowRgb(200));
        assertEquals(0x55FF55, ComboMilestones.glowRgb(250));
        assertEquals(0xFFD700, ComboMilestones.glowRgb(300));
        // 虹色の段階は数字の色、500 ごとは金
        assertEquals(-1, ComboMilestones.glowRgb(400));
        assertEquals(0xFFD700, ComboMilestones.glowRgb(500));
        assertEquals(0xFFD700, ComboMilestones.glowRgb(1000));
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

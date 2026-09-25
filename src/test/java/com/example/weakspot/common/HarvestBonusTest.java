package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public class HarvestBonusTest {

    @Test
    public void noBonusWithoutCombo() {
        assertEquals(3, HarvestBonus.apply(3, 1.0, new Random(1)));
        assertEquals(0, HarvestBonus.apply(0, 4.0, new Random(1)));
    }

    @Test
    public void wholeFactorsMultiply() {
        assertEquals(4, HarvestBonus.apply(1, 4.0, new Random(1)));
        assertEquals(6, HarvestBonus.apply(3, 2.0, new Random(1)));
    }

    @Test
    public void fractionsBecomeAChance() {
        Random random = new Random(42);
        int extra = 0;
        int trials = 10000;
        for (int i = 0; i < trials; i++) {
            int n = HarvestBonus.apply(1, 1.25, random);
            assertTrue(n == 1 || n == 2);
            extra += n - 1;
        }
        // 4 回に 1 回くらい +1
        assertEquals(0.25, extra / (double) trials, 0.02);
    }
}

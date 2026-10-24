package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ItemRulesTest {

    @Test
    public void shardChanceGrowsWithTheComboFactor() {
        assertEquals(0.005, ItemRules.shardChance(0.5, 1), 1e-12);
        assertEquals(0.01, ItemRules.shardChance(0.5, 100), 1e-12);
        assertEquals(0.025, ItemRules.shardChance(0.5, 500), 1e-12);
    }

    @Test
    public void zeroPercentNeverDrops() {
        assertFalse(ItemRules.dropsShard(0, 10000, 0.0));
        assertFalse(ItemRules.dropsShard(-3, 10000, 0.0));
    }

    @Test
    public void chanceOverOneAlwaysDrops() {
        assertTrue(ItemRules.dropsShard(100, 25, 0.999999));
    }

    @Test
    public void dropsBelowTheChanceOnly() {
        assertTrue(ItemRules.dropsShard(0.5, 1, 0.0049));
        assertFalse(ItemRules.dropsShard(0.5, 1, 0.005));
    }

    @Test
    public void graceStretchesTheComboWait() {
        assertEquals(40, ItemRules.comboResetTicks(false));
        assertEquals(120, ItemRules.comboResetTicks(true));
    }

    @Test
    public void hourglassAddsFiveMinutesWithoutACap() {
        assertEquals(6000, ItemRules.hourglassDuration(0));
        assertEquals(9000, ItemRules.hourglassDuration(3000));
        assertEquals(Integer.MAX_VALUE, ItemRules.hourglassDuration(Integer.MAX_VALUE - 10));
    }

    @Test
    public void trinketStopsAtOneLeftWithoutBreaking() {
        assertTrue(ItemRules.usable(498, 500));
        assertFalse(ItemRules.usable(499, 500));
    }
}

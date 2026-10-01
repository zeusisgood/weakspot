package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TargetRulesTest {

    @Test
    public void threePhasesOfTenSeconds() {
        assertEquals(0, TargetRules.phase(0));
        assertEquals(0, TargetRules.phase(199));
        assertEquals(1, TargetRules.phase(200));
        assertEquals(2, TargetRules.phase(400));
        assertEquals(2, TargetRules.phase(599));
        assertEquals(2, TargetRules.phase(10000));
    }

    @Test
    public void laterPhasesAreHarder() {
        assertTrue(TargetRules.radius(0) > TargetRules.radius(1) && TargetRules.radius(1) > TargetRules.radius(2));
        assertEquals(0, TargetRules.speed(0), 0);
        assertTrue(TargetRules.speed(2) > TargetRules.speed(1));
        assertEquals(0, TargetRules.decoys(1));
        assertTrue(TargetRules.decoys(2) > 0);
    }

    @Test
    public void decoyCostsFiveButNeverBelowZero() {
        assertEquals(1, TargetRules.afterHit(0, false));
        assertEquals(5, TargetRules.afterHit(10, true));
        assertEquals(0, TargetRules.afterHit(3, true));
        assertEquals(0, TargetRules.afterHit(0, true));
    }

    @Test
    public void tiersByBest() {
        assertEquals(TargetRules.Tier.NONE, TargetRules.Tier.of(14));
        assertEquals(TargetRules.Tier.BRONZE, TargetRules.Tier.of(15));
        assertEquals(TargetRules.Tier.SILVER, TargetRules.Tier.of(30));
        assertEquals(TargetRules.Tier.GOLD, TargetRules.Tier.of(45));
        assertEquals(TargetRules.Tier.RAINBOW, TargetRules.Tier.of(60));
        assertEquals(TargetRules.Tier.RAINBOW, TargetRules.Tier.of(200));
        assertEquals(TargetRules.Tier.SILVER, TargetRules.Tier.BRONZE.next());
        assertNull(TargetRules.Tier.RAINBOW.next());
        assertTrue(TargetRules.Tier.GOLD.atLeast(TargetRules.Tier.SILVER));
    }
}

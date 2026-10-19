package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimedBoostTest {

    @Test
    public void lastsForTheGivenTicks() {
        TimedBoost boost = new TimedBoost();
        assertFalse(boost.isActive(0));
        boost.start(1.5, 40, 100);
        assertTrue(boost.isActive(139));
        assertFalse(boost.isActive(140));
        assertEquals(1.5, boost.multiplier(), 1e-9);
    }

    @Test
    public void remainingFractionShrinksSmoothlyWithinATick() {
        TimedBoost boost = new TimedBoost();
        boost.start(2, 40, 100);
        assertEquals(1.0, boost.remainingFraction(100, 0), 1e-9);
        assertEquals(0.5, boost.remainingFraction(110, 10), 1e-9);
        assertEquals(0.0, boost.remainingFraction(200, 0), 1e-9);
    }

    @Test
    public void clearStopsAtOnce() {
        TimedBoost boost = new TimedBoost();
        boost.start(2, 40, 100);
        boost.clear();
        assertFalse(boost.isActive(101));
        assertEquals(0.0, boost.remainingFraction(101, 0), 1e-9);
    }
}

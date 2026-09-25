package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ScheduledBoostTest {

    @Test
    public void advanceShortensTheRemainingTime() {
        assertEquals(1, ScheduledBoost.advance(4, 3));
        assertEquals(-1, ScheduledBoost.advance(2, 3));
        assertEquals(2, ScheduledBoost.advance(2, 0));
        assertEquals(2, ScheduledBoost.advance(2, -1));
    }

    @Test
    public void dispenseCountFollowsTheComboFactor() {
        double base = 4.0;
        assertEquals(2, ScheduledBoost.dispenseCount(base * 1.0, base));
        assertEquals(2, ScheduledBoost.dispenseCount(base * 1.25, base));
        assertEquals(2, ScheduledBoost.dispenseCount(base * 1.5, base));
        assertEquals(3, ScheduledBoost.dispenseCount(base * 2.0, base));
        assertEquals(3, ScheduledBoost.dispenseCount(base * 2.5, base));
        assertEquals(4, ScheduledBoost.dispenseCount(base * 3.0, base));
        assertEquals(5, ScheduledBoost.dispenseCount(base * 4.0, base));
    }

    @Test
    public void dispenseCountIsCapped() {
        assertEquals(ScheduledBoost.MAX_DISPENSES, ScheduledBoost.dispenseCount(400, 4));
        // 上限の設定で倍率が抑えられると、回数も減る
        assertEquals(2, ScheduledBoost.dispenseCount(4, 4));
        assertEquals(1, ScheduledBoost.dispenseCount(1, 0));
    }
}

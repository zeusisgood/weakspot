package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SleepTimeTest {

    @Test
    public void nightWindow() {
        assertFalse(SleepTime.isNight(6000));
        assertTrue(SleepTime.isNight(12541));
        assertTrue(SleepTime.isNight(18000));
        assertFalse(SleepTime.isNight(23460));
        assertTrue(SleepTime.isNight(24000 * 5 + 13000));
    }

    @Test
    public void advanceStopsAtTheNextMorning() {
        assertEquals(13200, SleepTime.advance(13000, 200));
        assertEquals(24000, SleepTime.advance(23900, 200));
        assertEquals(48000, SleepTime.advance(24000 + 23950, 200));
    }
}

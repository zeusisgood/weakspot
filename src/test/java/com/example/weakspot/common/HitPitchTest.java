package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HitPitchTest {

    @Test
    public void startsAtBaseAndEndsOneOctaveUp() {
        assertEquals(1.0F, HitPitch.forStreak(1), 1e-6);
        assertEquals(2.0F, HitPitch.forStreak(8), 1e-6);
    }

    @Test
    public void risesEachHitThenWrapsToBottom() {
        for (int i = 1; i < 8; i++) {
            assertTrue(HitPitch.forStreak(i + 1) > HitPitch.forStreak(i));
        }
        assertEquals(1.0F, HitPitch.forStreak(9), 1e-6);
        assertEquals(HitPitch.forStreak(3), HitPitch.forStreak(11), 1e-6);
        assertEquals(2.0F, HitPitch.forStreak(16), 1e-6);
    }

    @Test
    public void thirdNoteIsMajorThird() {
        assertEquals(Math.pow(2, 4 / 12.0), HitPitch.forStreak(3), 1e-6);
    }
}

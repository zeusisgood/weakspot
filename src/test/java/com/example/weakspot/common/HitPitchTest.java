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

    @Test
    public void twoOctaveRunGoesFromHalfToDouble() {
        assertEquals(15, HitPitch.TWO_OCTAVE_LENGTH);
        assertEquals(0.5F, HitPitch.twoOctave(0), 1e-6);
        assertEquals(1.0F, HitPitch.twoOctave(7), 1e-6);
        assertEquals(2.0F, HitPitch.twoOctave(14), 1e-6);
        for (int i = 1; i < HitPitch.TWO_OCTAVE_LENGTH; i++) {
            assertTrue(HitPitch.twoOctave(i) > HitPitch.twoOctave(i - 1));
        }
        // 上の半分は、1オクターブの音階と同じ音
        for (int i = 7; i < HitPitch.TWO_OCTAVE_LENGTH; i++) {
            assertEquals(HitPitch.forStreak(i - 6), HitPitch.twoOctave(i), 1e-6);
        }
    }
}

package io.github.zeusisgood.weakspot.common;

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

    @Test
    public void hitIsOneNoteUntil24ThenStacksBelowTheMelody() {
        assertEquals(1, HitPitch.forHit(24, true).length);
        assertEquals(HitPitch.forStreak(24), HitPitch.forHit(24, true)[0], 1e-6);
        float[] two = HitPitch.forHit(25, true);
        assertEquals(2, two.length);
        assertEquals(HitPitch.forStreak(25), two[0], 1e-6);
        float[] three = HitPitch.forHit(100, true);
        assertEquals(3, three.length);
        assertEquals(HitPitch.forStreak(100), three[0], 1e-6);
        assertTrue(three[1] < three[0] && three[2] < three[1]);
    }

    @Test
    public void chordsOffKeepsOneNote() {
        assertEquals(1, HitPitch.forHit(500, false).length);
        assertEquals(HitPitch.forStreak(500), HitPitch.forHit(500, false)[0], 1e-6);
    }

    @Test
    public void stackedNotesAreAThirdAndAFifthBelowInTheScale() {
        // 旋律が上のド（2.0）: ラ（3 度下）と ファ（5 度下）
        float[] top = HitPitch.forHit(104, true);
        assertEquals(2.0F, top[0], 1e-6);
        assertEquals(Math.pow(2, 9 / 12.0), top[1], 1e-6);
        assertEquals(Math.pow(2, 5 / 12.0), top[2], 1e-6);
        // 旋律が下のド（1.0）: 下のラと下のファ
        float[] bottom = HitPitch.forHit(105, true);
        assertEquals(1.0F, bottom[0], 1e-6);
        assertEquals(Math.pow(2, -3 / 12.0), bottom[1], 1e-6);
        assertEquals(Math.pow(2, -7 / 12.0), bottom[2], 1e-6);
    }

    @Test
    public void everyPitchStaysInMinecraftsRange() {
        for (int streak = 1; streak <= 200; streak++) {
            for (float pitch : HitPitch.forHit(streak, true)) {
                assertTrue("streak " + streak + " pitch " + pitch, pitch >= 0.5F && pitch <= 2.0F);
            }
        }
    }

    @Test
    public void breakGoesDownFromLowSolToLowDo() {
        float[] notes = HitPitch.breakNotes();
        assertEquals(2, notes.length);
        assertEquals(Math.pow(2, -5 / 12.0), notes[0], 1e-6);
        assertEquals(0.5F, notes[1], 1e-6);
    }
}

package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HitPitchTest {

    @Test
    public void startsAtBaseAndEndsOneOctaveUp() {
        assertEquals(1.0F, HitScale.DEFAULT.melody(1), 1e-6);
        assertEquals(2.0F, HitScale.DEFAULT.melody(8), 1e-6);
    }

    @Test
    public void risesEachHitThenWrapsToBottom() {
        for (int i = 1; i < 8; i++) {
            assertTrue(HitScale.DEFAULT.melody(i + 1) > HitScale.DEFAULT.melody(i));
        }
        assertEquals(1.0F, HitScale.DEFAULT.melody(9), 1e-6);
        assertEquals(HitScale.DEFAULT.melody(3), HitScale.DEFAULT.melody(11), 1e-6);
        assertEquals(2.0F, HitScale.DEFAULT.melody(16), 1e-6);
    }

    @Test
    public void thirdNoteIsMajorThird() {
        assertEquals(Math.pow(2, 4 / 12.0), HitScale.DEFAULT.melody(3), 1e-6);
    }

    @Test
    public void twoOctaveRunGoesFromHalfToDouble() {
        assertEquals(15, HitPitch.TWO_OCTAVE_LENGTH);
        assertEquals(0.5F, HitScale.DEFAULT.twoOctave(0), 1e-6);
        assertEquals(1.0F, HitScale.DEFAULT.twoOctave(7), 1e-6);
        assertEquals(2.0F, HitScale.DEFAULT.twoOctave(14), 1e-6);
        for (int i = 1; i < HitPitch.TWO_OCTAVE_LENGTH; i++) {
            assertTrue(HitScale.DEFAULT.twoOctave(i) > HitScale.DEFAULT.twoOctave(i - 1));
        }
        // 上の半分は、1オクターブの音階と同じ音
        for (int i = 7; i < HitPitch.TWO_OCTAVE_LENGTH; i++) {
            assertEquals(HitScale.DEFAULT.melody(i - 6), HitScale.DEFAULT.twoOctave(i), 1e-6);
        }
    }

    @Test
    public void hitIsOneNoteUntil24ThenStacksBelowTheMelody() {
        assertEquals(1, HitScale.DEFAULT.forHit(24, true).length);
        assertEquals(HitScale.DEFAULT.melody(24), HitScale.DEFAULT.forHit(24, true)[0], 1e-6);
        float[] two = HitScale.DEFAULT.forHit(25, true);
        assertEquals(2, two.length);
        assertEquals(HitScale.DEFAULT.melody(25), two[0], 1e-6);
        float[] three = HitScale.DEFAULT.forHit(50, true);
        assertEquals(3, three.length);
        assertEquals(HitScale.DEFAULT.melody(50), three[0], 1e-6);
        assertTrue(three[1] < three[0] && three[2] < three[1]);
    }

    @Test
    public void chordsGrowWithTheComboSteps() {
        assertEquals(4, HitScale.DEFAULT.forHit(75, true).length);
        assertEquals(4, HitScale.DEFAULT.forHit(100, true).length);
        assertEquals(4, HitScale.DEFAULT.forHit(150, true).length);
        assertEquals(5, HitScale.DEFAULT.forHit(200, true).length);
        assertEquals(3, HitScale.DEFAULT.forHit(250, true).length);
        assertEquals(5, HitScale.DEFAULT.forHit(300, true).length);
        // 400 から 6・7・9・sus4 の形を巡る
        assertEquals(4, HitScale.DEFAULT.forHit(400, true).length);
        assertEquals(4, HitScale.DEFAULT.forHit(500, true).length);
        assertEquals(5, HitScale.DEFAULT.forHit(600, true).length);
        assertEquals(3, HitScale.DEFAULT.forHit(700, true).length);
        assertEquals(4, HitScale.DEFAULT.forHit(800, true).length);
    }

    @Test
    public void sus4ResolvesAndBellsFrom300() {
        assertTrue(HitScale.DEFAULT.resolveFor(250, true) > 0);
        assertEquals(0, HitScale.DEFAULT.resolveFor(249, true), 0);
        assertEquals(0, HitScale.DEFAULT.resolveFor(250, false), 0);
        assertTrue(HitScale.DEFAULT.resolveFor(700, true) > 0);
        assertTrue(HitPitch.withBell(300, true));
        assertTrue(!HitPitch.withBell(299, true) && !HitPitch.withBell(300, false));
    }

    @Test
    public void chordsOffKeepsOneNote() {
        assertEquals(1, HitScale.DEFAULT.forHit(500, false).length);
        assertEquals(HitScale.DEFAULT.melody(500), HitScale.DEFAULT.forHit(500, false)[0], 1e-6);
    }

    @Test
    public void stackedNotesAreAThirdAndAFifthBelowInTheScale() {
        // 旋律が上のド（2.0）: ラ（3 度下）と ファ（5 度下）
        float[] top = HitScale.DEFAULT.forHit(56, true);
        assertEquals(2.0F, top[0], 1e-6);
        assertEquals(Math.pow(2, 9 / 12.0), top[1], 1e-6);
        assertEquals(Math.pow(2, 5 / 12.0), top[2], 1e-6);
        // 旋律が下のド（1.0）: 下のラと下のファ
        float[] bottom = HitScale.DEFAULT.forHit(57, true);
        assertEquals(1.0F, bottom[0], 1e-6);
        assertEquals(Math.pow(2, -3 / 12.0), bottom[1], 1e-6);
        assertEquals(Math.pow(2, -7 / 12.0), bottom[2], 1e-6);
    }

    @Test
    public void everyPitchStaysInMinecraftsRange() {
        for (int streak = 1; streak <= 1200; streak++) {
            for (float pitch : HitScale.DEFAULT.forHit(streak, true)) {
                assertTrue("streak " + streak + " pitch " + pitch, pitch >= 0.5F && pitch <= 2.0F);
            }
        }
    }

    @Test
    public void breakGoesDownFromLowSolToLowDo() {
        float[] notes = HitScale.DEFAULT.breakNotes();
        assertEquals(2, notes.length);
        assertEquals(Math.pow(2, -5 / 12.0), notes[0], 1e-6);
        assertEquals(0.5F, notes[1], 1e-6);
    }
}

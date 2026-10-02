package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HitScaleTest {

    private static final HitScale UP_DOWN = new HitScale(HitScale.Type.MAJOR, HitScale.Direction.UP_DOWN, 1);
    private static final HitScale TWO = new HitScale(HitScale.Type.MAJOR, HitScale.Direction.UP, 2);

    /** 初期値は 1.11.0 までと同じ鳴り方（HitPitch の静的な関数と同じ）。 */
    @Test
    public void defaultMatchesHitPitch() {
        for (int streak = 1; streak <= 1000; streak++) {
            assertEquals(HitPitch.forStreak(streak), HitScale.DEFAULT.melody(streak), 0);
            assertArrayEquals(HitPitch.forHit(streak, true), HitScale.DEFAULT.forHit(streak, true), 0);
        }
        assertEquals(8, HitScale.DEFAULT.cycleLength());
    }

    /** 往復: ド → 上のド → ド（両端は 1 回）。周期は 14。 */
    @Test
    public void upDownTurnsAtBothEnds() {
        assertEquals(14, UP_DOWN.cycleLength());
        assertEquals(1.0F, UP_DOWN.melody(1), 1e-6);
        assertEquals(2.0F, UP_DOWN.melody(8), 1e-6);
        assertEquals(UP_DOWN.melody(7), UP_DOWN.melody(9), 1e-6);
        assertEquals(UP_DOWN.melody(2), UP_DOWN.melody(14), 1e-6);
        assertEquals(1.0F, UP_DOWN.melody(15), 1e-6);
        for (int s = 1; s < 8; s++) {
            assertTrue(UP_DOWN.melody(s + 1) > UP_DOWN.melody(s));
        }
        for (int s = 8; s < 15; s++) {
            assertTrue(UP_DOWN.melody(s + 1) < UP_DOWN.melody(s));
        }
    }

    /** 2 オクターブ: 0.5 から 2.0 まで 15 音。往復なら周期 28。 */
    @Test
    public void twoOctavesStartOneOctaveLower() {
        assertEquals(15, TWO.melodyLength());
        assertEquals(0.5F, TWO.melody(1), 1e-6);
        assertEquals(1.0F, TWO.melody(8), 1e-6);
        assertEquals(2.0F, TWO.melody(15), 1e-6);
        assertEquals(0.5F, TWO.melody(16), 1e-6);
        assertEquals(28, new HitScale(HitScale.Type.MAJOR, HitScale.Direction.UP_DOWN, 2).cycleLength());
    }

    /** ペンタトニックは 6 音（ド レ ミ ソ ラ ド）、短調はミ♭。 */
    @Test
    public void scaleTypes() {
        HitScale penta = new HitScale(HitScale.Type.PENTATONIC, HitScale.Direction.UP, 1);
        assertEquals(6, penta.melodyLength());
        assertEquals(Math.pow(2, 7 / 12.0), penta.melody(4), 1e-6);
        assertEquals(2.0F, penta.melody(6), 1e-6);
        HitScale minor = new HitScale(HitScale.Type.MINOR, HitScale.Direction.UP, 1);
        assertEquals(Math.pow(2, 3 / 12.0), minor.melody(3), 1e-6);
    }

    /** 和音と途切れの音は、どの組み合わせでもピッチ 0.5〜2.0 に収まる。 */
    @Test
    public void allNotesStayInRange() {
        for (HitScale.Type type : HitScale.Type.values()) {
            for (HitScale.Direction direction : HitScale.Direction.values()) {
                for (int octaves = 1; octaves <= 2; octaves++) {
                    HitScale scale = new HitScale(type, direction, octaves);
                    for (int streak = 1; streak <= 900; streak++) {
                        for (float pitch : scale.forHit(streak, true)) {
                            assertTrue(pitch >= 0.5F - 1e-6 && pitch <= 2.0F + 1e-6);
                        }
                        float resolve = scale.resolveFor(streak, true);
                        assertTrue(resolve == 0 || resolve >= 0.5F - 1e-6 && resolve <= 2.0F + 1e-6);
                    }
                    float[] notes = scale.breakNotes();
                    assertTrue(notes[0] > notes[1] && notes[1] >= 0.5F - 1e-6);
                    assertEquals(0.5F, scale.twoOctave(0), 1e-6);
                    assertEquals(2.0F, scale.twoOctave(scale.twoOctaveLength() - 1), 1e-6);
                }
            }
        }
    }

    /** 音域の設定が範囲の外なら、端に寄せる。 */
    @Test
    public void octavesAreClamped() {
        assertEquals(1, new HitScale(null, null, 0).octaves);
        assertEquals(2, new HitScale(HitScale.Type.MAJOR, HitScale.Direction.UP, 5).octaves);
    }
}

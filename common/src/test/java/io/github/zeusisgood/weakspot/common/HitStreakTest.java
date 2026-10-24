package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HitStreakTest {

    @Test
    public void continuesWithin40TicksAndResetsAfter41() {
        HitStreak streak = new HitStreak();
        assertEquals(1, streak.hit(100));
        assertEquals(2, streak.hit(140));
        assertEquals(2, streak.count(180));
        assertEquals(0, streak.count(181));
        assertEquals(1, streak.hit(181));
    }

    @Test
    public void expireReportsTheBrokenCountOnce() {
        HitStreak streak = new HitStreak();
        streak.hit(0);
        streak.hit(10);
        streak.hit(20);
        assertEquals(0, streak.expire(60));
        assertEquals(3, streak.count(60));
        assertEquals(3, streak.expire(61));
        assertEquals(0, streak.expire(62));
        assertEquals(0, streak.count(62));
        assertEquals(1, streak.hit(62));
    }

    @Test
    public void keepsRisingPastOneOctaveWhilePitchWraps() {
        HitStreak streak = new HitStreak();
        int count = 0;
        for (int i = 0; i < 20; i++) {
            count = streak.hit(i * 6);
        }
        assertEquals(20, count);
        assertEquals(HitPitch.forStreak(1), HitPitch.forStreak(HitPitch.SCALE_LENGTH + 1), 1e-6);
        assertEquals(2.0F, HitPitch.forStreak(HitPitch.SCALE_LENGTH), 1e-6);
        assertEquals(HitPitch.forStreak(4), HitPitch.forStreak(count), 1e-6);
        assertTrue(HitPitch.forStreak(9) < HitPitch.forStreak(8));
    }

    @Test
    public void resetStartsOver() {
        HitStreak streak = new HitStreak();
        streak.hit(0);
        streak.hit(5);
        streak.reset();
        assertEquals(0, streak.count(6));
        assertEquals(0, streak.expire(100));
        assertEquals(1, streak.hit(7));
    }

    @Test
    public void remainingFractionShrinksFromFullToEmpty() {
        assertEquals(1.0, HitStreak.remainingFraction(0, 40), 1e-9);
        assertEquals(0.5, HitStreak.remainingFraction(20, 40), 1e-9);
        assertEquals(0.0, HitStreak.remainingFraction(40, 40), 1e-9);
        assertEquals(0.0, HitStreak.remainingFraction(55, 40), 1e-9);
        assertEquals(1.0, HitStreak.remainingFraction(-1, 40), 1e-9);
        assertEquals(0.5, HitStreak.remainingFraction(60, 120), 1e-9);
    }

    @Test
    public void graceKeepsTheStreakFor120Ticks() {
        HitStreak streak = new HitStreak();
        streak.setResetTicks(ItemRules.comboResetTicks(true));
        assertEquals(1, streak.hit(100));
        assertEquals(2, streak.hit(220));
        assertEquals(2, streak.count(340));
        assertEquals(0, streak.expire(340));
        assertEquals(2, streak.expire(341));
    }

    @Test
    public void graceEndingShortensTheWaitAgain() {
        HitStreak streak = new HitStreak();
        streak.setResetTicks(ItemRules.comboResetTicks(true));
        streak.hit(0);
        streak.setResetTicks(ItemRules.comboResetTicks(false));
        assertEquals(0, streak.count(41));
    }
}

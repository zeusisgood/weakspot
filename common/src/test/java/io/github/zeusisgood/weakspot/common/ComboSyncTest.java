package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComboSyncTest {

    @Test
    public void reservationCountsAsOneTheServerHasNotCountedYet() {
        assertEquals(5, ComboSync.expected(5, false));
        assertEquals(6, ComboSync.expected(5, true));
        assertFalse(ComboSync.mismatched(6, 5, true));
        assertTrue(ComboSync.mismatched(6, 5, false));
        assertTrue(ComboSync.mismatched(7, 5, true));
    }

    @Test
    public void rejectedHitIsAMismatch() {
        // クライアントは 4 まで数え、サーバーは 4 つ目を受け付けなかった
        assertTrue(ComboSync.mismatched(4, 3, false));
        assertFalse(ComboSync.mismatched(3, 3, false));
    }

    @Test
    public void serverSendsOnlyAfterIdleAndNotTooOften() {
        assertFalse(ComboSync.canSend(102, 100, Long.MIN_VALUE / 2));
        assertTrue(ComboSync.canSend(103, 100, Long.MIN_VALUE / 2));
        assertFalse(ComboSync.canSend(103, 100, 100));
        assertTrue(ComboSync.canSend(104, 100, 100));
    }

    @Test
    public void clientCorrectsOnlyAfterIdleAndWhileTheStreakLasts() {
        assertFalse(ComboSync.canCorrect(102, 100, 5));
        assertTrue(ComboSync.canCorrect(103, 100, 5));
        assertFalse(ComboSync.canCorrect(103, 100, 0));
    }

    @Test
    public void correctKeepsTheLastHitTick() {
        HitStreak streak = new HitStreak();
        streak.hit(0);
        streak.hit(10);
        streak.hit(20);
        streak.correct(2);
        assertEquals(2, streak.count(30));
        assertEquals(20, streak.lastHitTick());
        assertEquals(3, streak.hit(30));
        // 途切れる時刻は最後のヒットから数える
        assertEquals(3, streak.count(70));
        assertEquals(0, streak.count(71));
    }

    @Test
    public void correctToZeroDoesNotReportABreak() {
        HitStreak streak = new HitStreak();
        streak.hit(0);
        streak.hit(10);
        streak.correct(0);
        assertEquals(0, streak.count(20));
        assertEquals(0, streak.expire(100));
        assertEquals(1, streak.hit(100));
    }
}

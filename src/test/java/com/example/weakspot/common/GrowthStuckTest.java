package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import com.example.weakspot.common.GrowthStuck.Warning;
import org.junit.Test;

public class GrowthStuckTest {

    private static final long A = 1;
    private static final long B = 2;

    @Test
    public void darkWarnsOnTheFirstHit() {
        GrowthStuck stuck = new GrowthStuck();
        assertEquals(Warning.DARK, stuck.onHit(A, false, true, 30, 1000));
    }

    @Test
    public void unknownWarnsAfterTheGivenHits() {
        GrowthStuck stuck = new GrowthStuck();
        long tick = 1000;
        for (int i = 1; i < 30; i++) {
            assertEquals(Warning.NONE, stuck.onHit(A, false, false, 30, tick += 6));
        }
        assertEquals(Warning.STUCK, stuck.onHit(A, false, false, 30, tick += 6));
        assertEquals(0, stuck.unchangedHits());
    }

    @Test
    public void aChangeStartsOver() {
        GrowthStuck stuck = new GrowthStuck();
        long tick = 1000;
        for (int i = 0; i < 29; i++) {
            stuck.onHit(A, false, false, 30, tick += 6);
        }
        assertEquals(Warning.NONE, stuck.onHit(A, true, false, 30, tick += 6));
        assertEquals(Warning.NONE, stuck.onHit(A, false, false, 30, tick += 6));
        assertEquals(1, stuck.unchangedHits());
    }

    @Test
    public void anotherBlockStartsOver() {
        GrowthStuck stuck = new GrowthStuck();
        long tick = 1000;
        for (int i = 0; i < 29; i++) {
            stuck.onHit(A, false, false, 30, tick += 6);
        }
        assertEquals(Warning.NONE, stuck.onHit(B, false, false, 30, tick += 6));
        assertEquals(1, stuck.unchangedHits());
    }

    @Test
    public void sameBlockWarnsOncePerThirtySeconds() {
        GrowthStuck stuck = new GrowthStuck();
        assertEquals(Warning.DARK, stuck.onHit(A, false, true, 30, 1000));
        assertEquals(Warning.NONE, stuck.onHit(A, false, true, 30, 1000 + GrowthStuck.SAME_BLOCK_COOLDOWN - 1));
        assertEquals(Warning.DARK, stuck.onHit(A, false, true, 30, 1000 + GrowthStuck.SAME_BLOCK_COOLDOWN));
    }

    @Test
    public void anyBlockWarnsOncePerFiveSeconds() {
        GrowthStuck stuck = new GrowthStuck();
        assertEquals(Warning.DARK, stuck.onHit(A, false, true, 30, 1000));
        assertEquals(Warning.NONE, stuck.onHit(B, false, true, 30, 1000 + GrowthStuck.ANY_COOLDOWN - 1));
        assertEquals(Warning.DARK, stuck.onHit(B, false, true, 30, 1000 + GrowthStuck.ANY_COOLDOWN));
    }

    @Test
    public void unknownWarnsAgainAfterMoreHitsAndTheCooldown() {
        GrowthStuck stuck = new GrowthStuck();
        long tick = 1000;
        int warnings = 0;
        // 6 tick ごとに 300 回（1800 tick）。30 回ごとに条件を満たすが、同じブロックは 600 tick に1回まで
        for (int i = 0; i < 300; i++) {
            if (stuck.onHit(A, false, false, 30, tick += 6) == Warning.STUCK) {
                warnings++;
            }
        }
        assertEquals(3, warnings);
    }
}

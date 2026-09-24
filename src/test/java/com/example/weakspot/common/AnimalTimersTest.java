package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AnimalTimersTest {

    @Test
    public void ticksPerHitDividesTheFullTime() {
        assertEquals(600, AnimalTimers.ticksPerHit(AnimalTimers.BABY_TICKS, 40));
        assertEquals(600, AnimalTimers.ticksPerHit(AnimalTimers.BREEDING_TICKS, 10));
        assertEquals(600, AnimalTimers.ticksPerHit(AnimalTimers.EGG_TICKS, 15));
    }

    @Test
    public void zeroOrNegativeHitsDisableTheTimer() {
        assertEquals(0, AnimalTimers.ticksPerHit(24000, 0));
        assertEquals(0, AnimalTimers.ticksPerHit(24000, -5));
    }

    @Test
    public void babyNeverPassesZero() {
        assertEquals(-23400, AnimalTimers.babyAfterHit(-24000, 600));
        assertEquals(0, AnimalTimers.babyAfterHit(-100, 600));
        assertEquals(0, AnimalTimers.babyAfterHit(0, 600));
        assertEquals(500, AnimalTimers.babyAfterHit(500, 600));
    }

    @Test
    public void fortyHitsRaiseABabyExactly() {
        int age = -AnimalTimers.BABY_TICKS;
        int step = AnimalTimers.ticksPerHit(AnimalTimers.BABY_TICKS, 40);
        for (int i = 0; i < 39; i++) {
            age = AnimalTimers.babyAfterHit(age, step);
            assertTrue(age < 0);
        }
        assertEquals(0, AnimalTimers.babyAfterHit(age, step));
    }

    @Test
    public void breedingCooldownNeverGoesBelowZero() {
        assertEquals(5400, AnimalTimers.breedingAfterHit(6000, 600));
        assertEquals(0, AnimalTimers.breedingAfterHit(300, 600));
        assertEquals(0, AnimalTimers.breedingAfterHit(0, 600));
        assertEquals(-24000, AnimalTimers.breedingAfterHit(-24000, 600));
    }

    @Test
    public void eggTimerCanReachZeroOrBelow() {
        assertEquals(400, AnimalTimers.eggAfterHit(1000, 600));
        assertTrue(AnimalTimers.eggAfterHit(500, 600) <= 0);
    }

    @Test
    public void hitCounterFiresOnceAtTheNeededHitAndRestarts() {
        AnimalTimers.HitCounter counter = new AnimalTimers.HitCounter();
        for (int i = 0; i < 9; i++) {
            assertFalse(counter.hit(10));
        }
        assertEquals(9, counter.count());
        assertTrue(counter.hit(10));
        assertEquals(0, counter.count());
        assertFalse(counter.hit(10));
        assertEquals(1, counter.count());
    }

    @Test
    public void hitCounterWithZeroNeededNeverFires() {
        AnimalTimers.HitCounter counter = new AnimalTimers.HitCounter();
        assertFalse(counter.hit(0));
        assertEquals(0, counter.count());
    }

    @Test
    public void barShowsTheFirstTimerInDisplayOrder() {
        int all = 0;
        for (AnimalTimers.Timer timer : AnimalTimers.Timer.values()) {
            all |= timer.bit();
        }
        assertEquals(AnimalTimers.Timer.BABY, AnimalTimers.barTimer(all));
        int noBaby = all & ~AnimalTimers.Timer.BABY.bit();
        assertEquals(AnimalTimers.Timer.WOOL, AnimalTimers.barTimer(noBaby));
        int noWool = noBaby & ~AnimalTimers.Timer.WOOL.bit();
        assertEquals(AnimalTimers.Timer.TRADE, AnimalTimers.barTimer(noWool));
        int noTrade = noWool & ~AnimalTimers.Timer.TRADE.bit();
        assertEquals(AnimalTimers.Timer.BREEDING, AnimalTimers.barTimer(noTrade));
        assertEquals(AnimalTimers.Timer.EGG,
                AnimalTimers.barTimer(AnimalTimers.Timer.EGG.bit()));
        assertNull(AnimalTimers.barTimer(0));
    }

    @Test
    public void breedingChickenShowsBreedingBeforeEgg() {
        int mask = AnimalTimers.Timer.BREEDING.bit() | AnimalTimers.Timer.EGG.bit();
        assertEquals(AnimalTimers.Timer.BREEDING, AnimalTimers.barTimer(mask));
    }

    @Test
    public void progressIsBetweenZeroAndOne() {
        assertEquals(0.0, AnimalTimers.babyProgress(-24000), 1e-9);
        assertEquals(0.5, AnimalTimers.babyProgress(-12000), 1e-9);
        assertEquals(0.0, AnimalTimers.babyProgress(-99999), 1e-9);
        assertEquals(0.0, AnimalTimers.breedingProgress(6000), 1e-9);
        assertEquals(1.0, AnimalTimers.breedingProgress(0), 1e-9);
        assertEquals(0.0, AnimalTimers.eggProgress(12000), 1e-9);
        assertEquals(1.0, AnimalTimers.eggProgress(-5), 1e-9);
        assertEquals(0.3, AnimalTimers.hitProgress(3, 10), 1e-9);
        assertEquals(0.0, AnimalTimers.hitProgress(3, 0), 1e-9);
    }

    @Test
    public void sneakIsRequiredWhenAnyConflictApplies() {
        assertFalse(AnimalTimers.requiresSneak(false, false, false, false, false));
        assertTrue(AnimalTimers.requiresSneak(true, false, false, false, false));
        assertTrue(AnimalTimers.requiresSneak(false, true, false, false, false));
        assertTrue(AnimalTimers.requiresSneak(false, false, true, false, false));
        assertTrue(AnimalTimers.requiresSneak(false, false, false, true, false));
        assertTrue(AnimalTimers.requiresSneak(false, false, false, false, true));
    }
}

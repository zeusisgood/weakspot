package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.github.zeusisgood.weakspot.common.VillagerBreeding.Mate;
import io.github.zeusisgood.weakspot.common.VillagerBreeding.Reason;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class VillagerBreedingTest {

    @Test
    public void doorsMatchVanilla() {
        assertEquals(2, VillagerBreeding.villagerLimit(8));
        assertEquals(3, VillagerBreeding.villagerLimit(10));
        assertTrue(VillagerBreeding.enoughDoors(2, 10));
        assertFalse(VillagerBreeding.enoughDoors(3, 10));
        assertFalse(VillagerBreeding.enoughDoors(0, 2));
    }

    @Test
    public void doorsNeeded() {
        assertEquals(0, VillagerBreeding.doorsNeeded(2, 10));
        // 村人 4 人: (int)(d × 0.35) > 4 になる一番小さい d は 15
        assertEquals(7, VillagerBreeding.doorsNeeded(4, 8));
        assertEquals(3, VillagerBreeding.doorsNeeded(0, 0));
    }

    @Test
    public void threeBreadOrTwelvePotatoesOrCarrotsAreEnough() {
        assertTrue(VillagerBreeding.hasFood(3, 0, 0));
        assertTrue(VillagerBreeding.hasFood(0, 12, 0));
        assertTrue(VillagerBreeding.hasFood(0, 0, 12));
        assertFalse(VillagerBreeding.hasFood(2, 11, 11));
    }

    @Test
    public void clockShowsMinutesAndSecondsRoundedUp() {
        assertEquals("5:00", VillagerBreeding.clock(6000));
        assertEquals("0:01", VillagerBreeding.clock(1));
        assertEquals("1:05", VillagerBreeding.clock(1300));
        assertEquals("0:00", VillagerBreeding.clock(0));
    }

    @Test
    public void noVillageStopsThere() {
        assertEquals(Collections.singletonList(Reason.NO_VILLAGE),
                VillagerBreeding.reasons(false, false, 9, 0, true, false, Mate.NONE));
    }

    @Test
    public void listsEveryUnmetCondition() {
        assertEquals(Arrays.asList(Reason.NOT_SEASON, Reason.DOORS, Reason.COOLDOWN, Reason.FOOD, Reason.MATE_CHILD),
                VillagerBreeding.reasons(true, false, 4, 8, true, false, Mate.CHILD));
        assertTrue(VillagerBreeding.reasons(true, true, 2, 10, false, true, Mate.READY).isEmpty());
    }

    @Test
    public void mateIsCheckedForChildThenCooldownThenFood() {
        assertEquals(Mate.NONE, VillagerBreeding.mate(false, true, true, false));
        assertEquals(Mate.CHILD, VillagerBreeding.mate(true, true, true, false));
        assertEquals(Mate.COOLDOWN, VillagerBreeding.mate(true, false, true, false));
        assertEquals(Mate.NO_FOOD, VillagerBreeding.mate(true, false, false, false));
        assertEquals(Mate.READY, VillagerBreeding.mate(true, false, false, true));
    }

    @Test
    public void withBedsAFreeBedReplacesTheVillageAndDoors() {
        assertEquals(Arrays.asList(Reason.NO_BED, Reason.COOLDOWN, Reason.FOOD, Reason.NO_MATE),
                VillagerBreeding.reasonsWithBeds(false, true, false, Mate.NONE));
        assertTrue(VillagerBreeding.reasonsWithBeds(true, false, true, Mate.READY).isEmpty());
    }

    @Test
    public void twelveFoodPointsAreEnough() {
        assertFalse(VillagerBreeding.hasFoodPoints(11));
        assertTrue(VillagerBreeding.hasFoodPoints(12));
    }
}

package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MiningStatsTest {

    @Test
    public void recordsHitsAndBlocks() {
        MiningStats stats = new MiningStats();
        stats.recordHit(12);
        stats.recordHit(12);
        stats.recordBlockBroken(2);
        stats.recordBlockBroken(0);

        assertEquals(2, stats.hits);
        assertEquals(2, stats.blocksBroken);
        assertEquals(1, stats.blocksBrokenWithHit);
        assertEquals(2, stats.maxHitsOnBlock);
        assertEquals(1.0, stats.averageHitsPerBlock(), 1e-9);
        assertEquals(1.2, stats.savedSeconds(), 1e-9);
    }

    @Test
    public void growthAndMachineHitsDoNotCountAsMiningHits() {
        MiningStats stats = new MiningStats();
        stats.recordGrowthHit();
        stats.recordMachineHit();
        stats.recordMachineHit();
        assertEquals(0, stats.hits);
        assertEquals(1, stats.growthHits);
        assertEquals(2, stats.machineHits);
        assertEquals(0, stats.savedTicks, 1e-9);
    }

    @Test
    public void averageIsNaNWithoutBlocks() {
        assertTrue(Double.isNaN(new MiningStats().averageHitsPerBlock()));
    }

    @Test
    public void resetClearsEverything() {
        MiningStats stats = new MiningStats();
        stats.recordHit(12);
        stats.recordBlockBroken(1);
        stats.recordGrowthHit();
        stats.recordMachineHit();
        stats.reset();
        assertEquals(0, stats.hits);
        assertEquals(0, stats.blocksBroken);
        assertEquals(0, stats.maxHitsOnBlock);
        assertEquals(0, stats.savedTicks, 1e-9);
        assertEquals(0, stats.growthHits);
        assertEquals(0, stats.machineHits);
    }
}

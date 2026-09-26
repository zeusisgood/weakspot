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
        stats.recordKindHit(HitKind.GROWTH);
        stats.recordKindHit(HitKind.MACHINE);
        stats.recordKindHit(HitKind.MACHINE);
        assertEquals(0, stats.hits);
        assertEquals(1, stats.count(HitKind.GROWTH));
        assertEquals(2, stats.count(HitKind.MACHINE));
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
        stats.recordKindHit(HitKind.GROWTH);
        stats.recordKindHit(HitKind.MACHINE);
        stats.reset();
        assertEquals(0, stats.hits);
        assertEquals(0, stats.blocksBroken);
        assertEquals(0, stats.maxHitsOnBlock);
        assertEquals(0, stats.savedTicks, 1e-9);
        assertEquals(0, stats.count(HitKind.GROWTH));
        assertEquals(0, stats.count(HitKind.MACHINE));
    }

    @Test
    public void maxStreakKeepsTheLargestAndResets() {
        MiningStats stats = new MiningStats();
        stats.recordStreak(3);
        stats.recordStreak(12);
        stats.recordStreak(1);
        assertEquals(12, stats.maxStreak);
        stats.reset();
        assertEquals(0, stats.maxStreak);
    }

    @Test
    public void streakCountedAcrossKindsFeedsMaxStreak() {
        // 採掘・成長・機械のヒットを、種類に関係なく同じ HitStreak に入れる。40 tick あくと数え直し
        HitStreak streak = new HitStreak();
        MiningStats stats = new MiningStats();
        long[] ticks = {0, 6, 12, 18, 24, 100, 106};
        for (long tick : ticks) {
            stats.recordStreak(streak.hit(tick));
        }
        assertEquals(5, stats.maxStreak);
    }

    @Test
    public void animalHitsAreCountedSeparatelyAndReset() {
        MiningStats stats = new MiningStats();
        stats.recordKindHit(HitKind.ANIMAL);
        stats.recordKindHit(HitKind.ANIMAL);
        assertEquals(2, stats.count(HitKind.ANIMAL));
        assertEquals(0, stats.hits);
        stats.reset();
        assertEquals(0, stats.count(HitKind.ANIMAL));
    }

    @Test
    public void fishingHitsAreCountedSeparatelyAndReset() {
        MiningStats stats = new MiningStats();
        stats.recordKindHit(HitKind.FISHING);
        assertEquals(1, stats.count(HitKind.FISHING));
        assertEquals(0, stats.hits);
        stats.reset();
        assertEquals(0, stats.count(HitKind.FISHING));
    }

    @Test
    public void countsEachKindAndTheTotal() {
        MiningStats stats = new MiningStats();
        stats.recordHit(0);
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                stats.recordKindHit(kind);
            }
        }
        stats.recordKindHit(HitKind.HARVEST);
        for (HitKind kind : HitKind.values()) {
            assertEquals(kind == HitKind.HARVEST ? 2 : 1, stats.count(kind));
        }
        assertEquals(1, stats.count(HitKind.MELEE));
        assertEquals(HitKind.values().length + 1, stats.totalHits());
        stats.reset();
        assertEquals(0, stats.totalHits());
    }

    @Test
    public void saveKeysFollowTheKindNames() {
        assertEquals("hits", MiningStats.saveKey(HitKind.MINING));
        // 1.9.0 で critHits から改名。1.8.x のワールドは legacySaveKey で読む
        assertEquals("meleeHits", MiningStats.saveKey(HitKind.MELEE));
        assertEquals("critHits", MiningStats.legacySaveKey(HitKind.MELEE));
        assertEquals(null, MiningStats.legacySaveKey(HitKind.BOW));
        assertEquals("portalHits", MiningStats.saveKey(HitKind.PORTAL));
    }

    @Test
    public void writeAndReadRoundTrip() {
        MiningStats stats = new MiningStats();
        stats.recordHit(3);
        stats.recordBlockBroken(4);
        stats.recordStreak(9);
        for (HitKind kind : HitKind.values()) {
            for (int i = 0; i < kind.ordinal(); i++) {
                stats.recordKindHit(kind);
            }
        }
        java.util.ArrayDeque<Object> values = new java.util.ArrayDeque<>();
        stats.writeTo(new MiningStats.Writer() {
            @Override
            public void writeLong(long value) {
                values.add(value);
            }

            @Override
            public void writeDouble(double value) {
                values.add(value);
            }
        });
        MiningStats back = MiningStats.readFrom(new MiningStats.Reader() {
            @Override
            public long readLong() {
                return (Long) values.poll();
            }

            @Override
            public double readDouble() {
                return (Double) values.poll();
            }
        });
        assertTrue(values.isEmpty());
        assertEquals(stats.maxStreak, back.maxStreak);
        assertEquals(stats.savedTicks, back.savedTicks, 0);
        for (HitKind kind : HitKind.values()) {
            assertEquals(kind.name(), stats.count(kind), back.count(kind));
        }
    }
}

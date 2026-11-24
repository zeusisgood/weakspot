package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
    public void averageCountsOnlyHitsOnBrokenBlocks() {
        MiningStats stats = new MiningStats();
        for (int i = 0; i < 10; i++) {
            stats.recordHit(0);
        }
        stats.recordBlockBroken(3);
        stats.recordBlockBroken(5);
        assertEquals(8, stats.hitsOnBrokenBlocks);
        assertEquals(4.0, stats.averageHitsPerBlock(), 1e-9);
        assertEquals(5, stats.maxHitsOnBlock);
    }

    @Test
    public void averageIsNaNWithoutBlocks() {
        assertTrue(Double.isNaN(new MiningStats().averageHitsPerBlock()));
    }

    @Test
    public void maxStreakKeepsTheLargest() {
        MiningStats stats = new MiningStats();
        stats.recordStreak(3);
        stats.recordStreak(12);
        stats.recordStreak(1);
        assertEquals(12, stats.maxStreak);
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
    public void animalHitsAreCountedSeparately() {
        MiningStats stats = new MiningStats();
        stats.recordKindHit(HitKind.ANIMAL);
        stats.recordKindHit(HitKind.ANIMAL);
        assertEquals(2, stats.count(HitKind.ANIMAL));
        assertEquals(0, stats.hits);
    }

    @Test
    public void fishingHitsAreCountedSeparately() {
        MiningStats stats = new MiningStats();
        stats.recordKindHit(HitKind.FISHING);
        assertEquals(1, stats.count(HitKind.FISHING));
        assertEquals(0, stats.hits);
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
    }

    @Test
    public void saveKeysFollowTheKindNames() {
        assertEquals("hits", MiningStats.saveKey(HitKind.MINING));
        // 古いワールドのキー critHits は legacySaveKey で読む
        assertEquals("meleeHits", MiningStats.saveKey(HitKind.MELEE));
        assertEquals("critHits", MiningStats.legacySaveKey(HitKind.MELEE));
        assertEquals(null, MiningStats.legacySaveKey(HitKind.BOW));
        assertEquals("portalHits", MiningStats.saveKey(HitKind.PORTAL));
    }

    @Test
    public void writeAndReadRoundTrip() throws IOException {
        MiningStats stats = new MiningStats();
        stats.recordHit(3);
        stats.recordBlockBroken(4);
        stats.recordStreak(9);
        for (HitKind kind : HitKind.values()) {
            for (int i = 0; i < kind.ordinal(); i++) {
                stats.recordKindHit(kind);
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        stats.writeTo(new DataOutputStream(bytes));
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        MiningStats back = MiningStats.readFrom(in);
        assertEquals(0, in.available());
        assertEquals(stats.maxStreak, back.maxStreak);
        assertEquals(stats.savedTicks, back.savedTicks, 0);
        for (HitKind kind : HitKind.values()) {
            assertEquals(kind.name(), stats.count(kind), back.count(kind));
        }
    }

    /** 項目を足して save / load を書き忘れると落ちるよう、項目は名前で並べずに全部取る。 */
    @Test
    public void saveAndLoadKeepEveryField() throws Exception {
        MiningStats stats = new MiningStats();
        int value = 1;
        for (java.lang.reflect.Field field : MiningStats.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() == long.class) {
                field.setLong(stats, value++);
            } else if (field.getType() == double.class) {
                field.setDouble(stats, value++ + 0.5);
            } else {
                throw new AssertionError("保存の形が決まっていない型: " + field);
            }
        }
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                stats.setCount(kind, value++);
            }
        }
        MapStore store = new MapStore();
        stats.save(store);
        MiningStats back = MiningStats.load(store);
        for (java.lang.reflect.Field field : MiningStats.class.getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                assertEquals(field.getName(), field.get(stats), field.get(back));
            }
        }
        for (HitKind kind : HitKind.values()) {
            assertEquals(kind.name(), stats.count(kind), back.count(kind));
        }
    }

    /** キーを変えると古いワールドの統計が読めなくなる。 */
    @Test
    public void saveKeysUnchanged() {
        MapStore store = new MapStore();
        new MiningStats().save(store);
        assertEquals("[animalHits, blocksBroken, blocksBrokenWithHit, bowHits, eatHits, elytraHits, enchantHits,"
                + " fallHits, fishingHits, growthHits, harvestHits, hits, hitsOnBrokenBlocks, ladderHits, machineHits,"
                + " maxHitsOnBlock, maxStreak, meleeHits, portalHits, savedTicks, sleepHits, sprintHits, swimHits,"
                + " targetBest, targetRounds, throwHits, vehicleHits]", store.values.keySet().toString());
        assertTrue(store.values.get("savedTicks") instanceof Double);
        assertTrue(store.values.get("hits") instanceof Long);
    }

    @Test
    public void legacyMeleeKeyIsReadOnlyWithoutTheNewOne() {
        MapStore old = new MapStore();
        old.putLong("critHits", 42);
        assertEquals(42, MiningStats.load(old).count(HitKind.MELEE));
        old.putLong("meleeHits", 7);
        assertEquals(7, MiningStats.load(old).count(HitKind.MELEE));
    }

    @Test
    public void missingKeysLoadAsZero() {
        MiningStats stats = MiningStats.load(new MapStore());
        assertEquals(0, stats.totalHits());
        assertEquals(0, stats.savedTicks, 0);
    }

    private static final class MapStore implements MiningStats.Store {
        final java.util.TreeMap<String, Object> values = new java.util.TreeMap<>();

        @Override
        public boolean has(String key) {
            return values.containsKey(key);
        }

        @Override
        public long getLong(String key) {
            Object value = values.get(key);
            return value instanceof Long ? (Long) value : 0;
        }

        @Override
        public double getDouble(String key) {
            Object value = values.get(key);
            return value instanceof Double ? (Double) value : 0;
        }

        @Override
        public void putLong(String key, long value) {
            values.put(key, value);
        }

        @Override
        public void putDouble(String key, double value) {
            values.put(key, value);
        }
    }
}

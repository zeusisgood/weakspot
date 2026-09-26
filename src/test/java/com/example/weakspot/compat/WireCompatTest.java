package com.example.weakspot.compat;

import static org.junit.Assert.assertEquals;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MiningStats;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.network.StatsMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

/**
 * 送る中身（統計・設定）と保存のキーが、パッチの間に 1 バイトも変わらないことを確かめる（1.8.6 から）。
 * 期待値は 1.9.0 の実装で作り直したもの（1.8.x までは 1.8.5 の実装で作ったもの）。通信や保存の形を変える版（マイナー）では、
 * 期待値を作り直す。
 */
public class WireCompatTest {

    static final String STATS_HEX = "0000000000000001000000000000000200000000000000030000000000000004401600000000000000000000000000060000"
            + "0000000000020000000000000003000000000000000400000000000000050000000000000006000000000000000700000000"
            + "000000080000000000000009000000000000000a000000000000000b000000000000000c000000000000000d000000000000"
            + "000e000000000000000f00000000000000100000000000000011000000000000006500000000000000660000000000000067"
            + "0000000000000068405a600000000000000000000000006a0000000000000066000000000000006700000000000000680000"
            + "000000000069000000000000006a000000000000006b000000000000006c000000000000006d000000000000006e00000000"
            + "0000006f00000000000000700000000000000071000000000000007200000000000000730000000000000074000000000000"
            + "0075";
    static final String SETTINGS_HEX = "0276314002000000000000400a000000000000401100000000000040150000000000004019000000000000401d0000000000"
            + "0000000019000140268000000000000000002500000028010000002e40304000000000000000000203613137036231370000"
            + "00020361313803623138000000003d00000002036132310362323140364000000000004037400000000000010000004c0100"
            + "0100010000005e0000006100000064000000670000006a000000020361333603623336000000020361333703623337010000"
            + "007600000079000000007f00000082010000008840472000000000004047a000000000000100000094404920000000000040"
            + "49a000000000000000009d00000000a3000000a601000000ac01000000b2404e200000000000404ea00000000000000000bb"
            + "00000000c1405050000000000001000000ca01000000d001000000d6405210000000000000000000df4052d0000000000040"
            + "53100000000000000000e801000000ee000000f14054500000000000000000f7";
    static final String SAVE_KEYS = "{animalHits=4L, blocksBroken=2L, blocksBrokenWithHit=3L, bowHits=6L, eatHits=9L, elytraHits=12L, enc"
            + "hantHits=13L, fishingHits=5L, growthHits=2L, harvestHits=14L, hits=1L, ladderHits=11L, machineHits=3"
            + "L, maxHitsOnBlock=4L, maxStreak=6L, meleeHits=7L, portalHits=17L, savedTicks=5.5d, sleepHits=10L, sp"
            + "rintHits=16L, throwHits=15L, vehicleHits=8L}";

    static MiningStats stats(int base) {
        MiningStats stats = new MiningStats();
        stats.hits = base + 1;
        stats.blocksBroken = base + 2;
        stats.blocksBrokenWithHit = base + 3;
        stats.maxHitsOnBlock = base + 4;
        stats.savedTicks = base + 5.5;
        stats.maxStreak = base + 6;
        for (HitKind kind : HitKind.values()) {
            if (kind == HitKind.MINING) {
                continue;
            }
            for (int i = 0; i <= kind.ordinal() + base; i++) {
                stats.recordKindHit(kind);
            }
        }
        return stats;
    }

    static String hex(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            sb.append(String.format("%02x", buf.readByte()));
        }
        return sb.toString();
    }

    static SyncedSettings settings() throws Exception {
        Constructor<SyncedSettings> ctor = SyncedSettings.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        SyncedSettings s = ctor.newInstance();
        int i = 0;
        for (Field f : SyncedSettings.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || !Modifier.isPublic(f.getModifiers())) {
                continue;
            }
            i++;
            Class<?> t = f.getType();
            if (t == double.class) {
                f.setDouble(s, i + 0.25);
            } else if (t == int.class) {
                f.setInt(s, i * 3 + 1);
            } else if (t == boolean.class) {
                f.setBoolean(s, i % 2 == 0);
            } else if (t == String.class) {
                f.set(s, "v" + i);
            } else if (t == Set.class) {
                f.set(s, new LinkedHashSet<>(Arrays.asList("a" + i, "b" + i)));
            } else {
                throw new AssertionError("unknown type " + f);
            }
        }
        return s;
    }

    @Test
    public void statsBytesUnchanged() {
        ByteBuf buf = Unpooled.buffer();
        new StatsMessage(stats(0), stats(100)).toBytes(buf);
        assertEquals(STATS_HEX, hex(buf));
    }

    @Test
    public void settingsBytesUnchanged() throws Exception {
        ByteBuf buf = Unpooled.buffer();
        settings().write(buf);
        assertEquals(SETTINGS_HEX, hex(buf));
    }

    @Test
    public void settingsRoundTrip() throws Exception {
        ByteBuf buf = Unpooled.buffer();
        SyncedSettings original = settings();
        original.write(buf);
        SyncedSettings copy = SyncedSettings.read(buf);
        for (Field f : SyncedSettings.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())) {
                Object a = f.get(original);
                Object b = f.get(copy);
                assertEquals(f.getName(), a instanceof Set ? new java.util.HashSet<>((Set<?>) a) : a, b);
            }
        }
    }

    @Test
    public void saveKeysUnchanged() throws Exception {
        Class<?> serverStats = Class.forName("com.example.weakspot.server.ServerStats");
        Method write = serverStats.getDeclaredMethod("write", MiningStats.class);
        write.setAccessible(true);
        NBTTagCompound tag = (NBTTagCompound) write.invoke(null, stats(0));
        TreeMap<String, String> sorted = new TreeMap<>();
        for (String key : tag.getKeySet()) {
            NBTBase value = tag.getTag(key);
            sorted.put(key, value.toString());
        }
        assertEquals(SAVE_KEYS, sorted.toString());
        Method read = serverStats.getDeclaredMethod("read", NBTTagCompound.class);
        read.setAccessible(true);
        MiningStats back = (MiningStats) read.invoke(null, tag);
        for (HitKind kind : HitKind.values()) {
            assertEquals(kind.name(), stats(0).count(kind), back.count(kind));
        }
    }

    @Test
    public void legacyMeleeKeyIsRead() throws Exception {
        Class<?> serverStats = Class.forName("com.example.weakspot.server.ServerStats");
        Method read = serverStats.getDeclaredMethod("read", NBTTagCompound.class);
        read.setAccessible(true);
        NBTTagCompound old = new NBTTagCompound();
        old.setLong("critHits", 42);
        assertEquals(42, ((MiningStats) read.invoke(null, old)).count(HitKind.MELEE));
        // 新しいキーがあれば、そちらを読む
        old.setLong("meleeHits", 7);
        assertEquals(7, ((MiningStats) read.invoke(null, old)).count(HitKind.MELEE));
    }
}

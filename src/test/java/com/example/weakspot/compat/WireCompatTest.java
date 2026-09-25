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
 * 1.8.6 のリファクタで、送る中身（統計・設定）と保存のキーが 1 バイトも変わらないことを確かめる。
 * 期待値は 1.8.5 の実装で作ったもの。通信や保存の形を変える版（マイナー）では、期待値を作り直す。
 */
public class WireCompatTest {

    static final String STATS_HEX = "0000000000000001000000000000000200000000000000030000000000000004401600000000000000000000000000020000"
            + "0000000000030000000000000006000000000000000400000000000000050000000000000006000000000000000700000000"
            + "000000080000000000000009000000000000000a000000000000000b000000000000000c000000000000000d000000000000"
            + "000e000000000000000f00000000000000100000000000000011000000000000006500000000000000660000000000000067"
            + "0000000000000068405a60000000000000000000000000660000000000000067000000000000006a00000000000000680000"
            + "000000000069000000000000006a000000000000006b000000000000006c000000000000006d000000000000006e00000000"
            + "0000006f00000000000000700000000000000071000000000000007200000000000000730000000000000074000000000000"
            + "0075";
    static final String SETTINGS_HEX = "3ff4000000000000000000070000000a401100000000000040150000000000004019000000000000401d0000000000004020"
            + "80000000000040228000000000000000001f0000002240288000000000000000000203613133036231330000000203613134"
            + "036231340000002e00000002036131360362313600010001000000004300000046000000490000004c0000004f0000005200"
            + "0000020361323803623238000000020361323903623239010000005e0000006100000000670000006a010000007040432000"
            + "000000004043a00000000000010000007c0000007f4045a00000000000404620000000000003763735000000008b00000000"
            + "9100000094014049a00000000000404a200000000000000000a0000000a34053100000000000000000e800404c2000000000"
            + "00404ca00000000000000000af000000b201404ea00000000000000000bb00000000c100000000c700405110000000000000"
            + "0000d0014051d000000000004052100000000000000000dc000000df";
    static final String SAVE_KEYS = "{animalHits=4L, blocksBroken=2L, blocksBrokenWithHit=3L, bowHits=6L, critHits=7L, eatHits=9L, elytra"
            + "Hits=12L, enchantHits=13L, fishingHits=5L, growthHits=2L, harvestHits=14L, hits=1L, ladderHits=11L, "
            + "machineHits=3L, maxHitsOnBlock=4L, maxStreak=6L, portalHits=17L, savedTicks=5.5d, sleepHits=10L, spr"
            + "intHits=16L, throwHits=15L, vehicleHits=8L}";

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
}

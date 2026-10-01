package io.github.zeusisgood.weakspot.compat;

import static org.junit.Assert.assertEquals;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.StatsMessage;
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
 * 期待値は 1.11.0 の実装で作り直したもの（1.10.x は 1.10.0、1.9.x は 1.9.0、1.8.x までは 1.8.5 の実装で作ったもの）。通信や保存の形を変える版（マイナー）では、
 * 期待値を作り直す。
 */
public class WireCompatTest {

    static final String STATS_HEX = "0000000000000001000000000000000200000000000000030000000000000007000000000000000440160000000000000000"
            + "0000000000060000000000000002000000000000000300000000000000040000000000000005000000000000000600000000"
            + "0000000700000000000000080000000000000009000000000000000a000000000000000b000000000000000c000000000000"
            + "000d000000000000000e000000000000000f0000000000000010000000000000001100000000000000120000000000000013"
            + "000000000000006500000000000000660000000000000067000000000000006b0000000000000068405a6000000000000000"
            + "00000000006a0000000000000066000000000000006700000000000000680000000000000069000000000000006a00000000"
            + "0000006b000000000000006c000000000000006d000000000000006e000000000000006f0000000000000070000000000000"
            + "0071000000000000007200000000000000730000000000000074000000000000007500000000000000760000000000000077";
    static final String SETTINGS_HEX = "0276314002000000000000400a000000000000401100000000000040150000000000004019000000000000401d0000000000"
            + "00000000194022800000000000010000000203613131036231310100402c8000000000000000002e00000031000000003740"
            + "3340000000000000000002036132300362323000000002036132310362323101000000460000000203613234036232344039"
            + "400000000000403a40000000000000000000550001000100000000670000006a0000006d0000007000000073000000020361"
            + "333903623339000000020361343003623430000000007f0000008201000000880000008b00000000914048a0000000000040"
            + "49200000000000000000009d404aa00000000000404b200000000000000000a601000000ac000000af00000000b500000000"
            + "bb404fa000000000004050100000000000000000c401000000ca405110000000000000000000d300000000d900000000df40"
            + "52d0000000000001000000e840539000000000004053d00000000000000000f100000000f7000000fa010000010040559000"
            + "000000004055d000000000000000010940565000000000000100000112405710000000000040575000000000004057900000"
            + "0000000000011e";
    static final String SAVE_KEYS = "{animalHits=4L, blocksBroken=2L, blocksBrokenWithHit=3L, bowHits=6L, eatHits=9L, elytraHits=12L, enc"
            + "hantHits=13L, fallHits=19L, fishingHits=5L, growthHits=2L, harvestHits=14L, hits=1L, hitsOnBrokenBlo"
            + "cks=7L, ladderHits=11L, machineHits=3L, maxHitsOnBlock=4L, maxStreak=6L, meleeHits=7L, portalHits=17"
            + "L, savedTicks=5.5d, sleepHits=10L, sprintHits=16L, swimHits=18L, throwHits=15L, vehicleHits=8L}";

    static MiningStats stats(int base) {
        MiningStats stats = new MiningStats();
        stats.hits = base + 1;
        stats.blocksBroken = base + 2;
        stats.blocksBrokenWithHit = base + 3;
        stats.hitsOnBrokenBlocks = base + 7;
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
        Class<?> serverStats = Class.forName("io.github.zeusisgood.weakspot.server.ServerStats");
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
        Class<?> serverStats = Class.forName("io.github.zeusisgood.weakspot.server.ServerStats");
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

package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MiningStats;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ワールドに保存し（data/weakspot_leaderboard.dat）、オフラインの人も名前と数で並べられるようにする。プレイヤーの累計（ServerStats）から、
 * ログイン・ログアウト・的当ての終わり・順位を見る直前に写す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class Leaderboard extends SavedData {

    private static final String NAME = "weakspot_leaderboard";

    public enum Category {
        MINING, COMBO, TARGET, TOTAL;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Category byKey(String key) {
            for (Category c : values()) {
                if (c.key().equalsIgnoreCase(key)) {
                    return c;
                }
            }
            return null;
        }
    }

    public static final class Entry {
        public final UUID id;
        public String name = "";
        final long[] values = new long[Category.values().length];

        Entry(UUID id) {
            this.id = id;
        }

        public long value(Category category) {
            return values[category.ordinal()];
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    private Leaderboard() {
    }

    /** オーバーワールドの保存データから読む（なければ作る）。 */
    static Leaderboard get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Leaderboard::load, Leaderboard::new, NAME);
    }

    static void update(ServerPlayer player) {
        Leaderboard board = get(player.server);
        Entry entry = board.entries.computeIfAbsent(player.getUUID(), Entry::new);
        MiningStats total = ServerStats.total(player);
        entry.name = player.getName().getString();
        entry.values[Category.MINING.ordinal()] = total.hits;
        entry.values[Category.COMBO.ordinal()] = total.maxStreak;
        entry.values[Category.TARGET.ordinal()] = total.targetBest;
        entry.values[Category.TOTAL.ordinal()] = total.totalHits();
        board.setDirty();
    }

    /** オンラインの全員を写してから並べる。0 の人は除き、同じ数は名前の順。 */
    static List<Entry> ranking(MinecraftServer server, Category category) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            update(player);
        }
        List<Entry> list = new ArrayList<>();
        for (Entry entry : get(server).entries.values()) {
            if (entry.value(category) > 0) {
                list.add(entry);
            }
        }
        list.sort(Comparator.comparingLong((Entry e) -> -e.value(category)).thenComparing(e -> e.name));
        return list;
    }

    /** だれもいなければ 0。写す前に読む（的当ての「1 位を更新」の判定）。 */
    static long best(MinecraftServer server, Category category) {
        long best = 0;
        for (Entry entry : get(server).entries.values()) {
            best = Math.max(best, entry.value(category));
        }
        return best;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            update(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            update(player);
        }
    }

    private static Leaderboard load(CompoundTag nbt) {
        Leaderboard board = new Leaderboard();
        ListTag list = nbt.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Entry entry = new Entry(tag.getUUID("id"));
            entry.name = tag.getString("name");
            for (Category c : Category.values()) {
                entry.values[c.ordinal()] = tag.getLong(c.key());
            }
            board.entries.put(entry.id, entry);
        }
        return board;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", entry.id);
            tag.putString("name", entry.name);
            for (Category c : Category.values()) {
                tag.putLong(c.key(), entry.value(c));
            }
            list.add(tag);
        }
        nbt.put("players", list);
        return nbt;
    }
}

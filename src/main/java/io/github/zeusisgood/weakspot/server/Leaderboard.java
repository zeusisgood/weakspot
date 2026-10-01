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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * サーバー内の順位の元データ（1.11.0。/weakspot top と、統計画面の「的当て」タブの上位 10 人）。ワールドに保存し
 * （data/weakspot_leaderboard.dat）、オフラインの人も名前と数で並べられるようにする。プレイヤーの累計（ServerStats）から、
 * ログイン・ログアウト・的当ての終わり・順位を見る直前に写す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class Leaderboard extends WorldSavedData {

    private static final String NAME = "weakspot_leaderboard";

    /** 並べる項目。 */
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

    /** 1 人分。 */
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

    public Leaderboard(String name) {
        super(name);
    }

    /** オーバーワールドの保存データから読む（なければ作る）。 */
    static Leaderboard get(MinecraftServer server) {
        World world = server.getWorld(0);
        MapStorage storage = world.getMapStorage();
        Leaderboard board = (Leaderboard) storage.getOrLoadData(Leaderboard.class, NAME);
        if (board == null) {
            board = new Leaderboard(NAME);
            storage.setData(NAME, board);
        }
        return board;
    }

    /** そのプレイヤーの今の累計を写す。 */
    static void update(EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Leaderboard board = get(server);
        Entry entry = board.entries.computeIfAbsent(player.getUniqueID(), Entry::new);
        MiningStats total = ServerStats.total(player);
        entry.name = player.getName();
        entry.values[Category.MINING.ordinal()] = total.hits;
        entry.values[Category.COMBO.ordinal()] = total.maxStreak;
        entry.values[Category.TARGET.ordinal()] = total.targetBest;
        entry.values[Category.TOTAL.ordinal()] = total.totalHits();
        board.markDirty();
    }

    /** オンラインの全員を写してから、その項目の上位（0 の人は除く。同じ数は名前の順）。 */
    static List<Entry> ranking(MinecraftServer server, Category category) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
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

    /** その項目の今の 1 位の数（だれもいなければ 0）。写す前に読む（的当ての「1 位を更新」の判定）。 */
    static long best(MinecraftServer server, Category category) {
        long best = 0;
        for (Entry entry : get(server).entries.values()) {
            best = Math.max(best, entry.value(category));
        }
        return best;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            update((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            update((EntityPlayerMP) event.player);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        entries.clear();
        NBTTagList list = nbt.getTagList("players", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            Entry entry = new Entry(tag.getUniqueId("id"));
            entry.name = tag.getString("name");
            for (Category c : Category.values()) {
                entry.values[c.ordinal()] = tag.getLong(c.key());
            }
            entries.put(entry.id, entry);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Entry entry : entries.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setUniqueId("id", entry.id);
            tag.setString("name", entry.name);
            for (Category c : Category.values()) {
                tag.setLong(c.key(), entry.value(c));
            }
            list.appendTag(tag);
        }
        nbt.setTag("players", list);
        return nbt;
    }
}

package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitStreak;
import com.example.weakspot.common.MiningStats;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * サーバーが記録する統計。累計はプレイヤーの永続データ（PlayerPersisted。死亡・ディメンション移動でも
 * 引き継がれる）に保存するので、ワールドごと・プレイヤーごとになる。「今回」はログインからログアウトまでで、メモリにだけ持つ。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ServerStats {

    private static final String TAG = WeakSpotMod.MODID;
    private static final String TAG_TOTAL = "total";
    /** 節目に使う採掘ヒットの累計。画面の「累計をリセット」では消さない（節目の報酬を何度も取れないように）。 */
    private static final String TAG_REWARD_HITS = "rewardHits";

    private static final Map<UUID, MiningStats> SESSIONS = new HashMap<>();
    /** 連続ヒット（コンボ）。クライアントのコンボと同じ条件で、サーバーの tick で数える。メモリだけ。 */
    private static final Map<UUID, HitStreak> STREAKS = new HashMap<>();

    private ServerStats() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SESSIONS.put(event.player.getUniqueID(), new MiningStats());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.player.getUniqueID());
        STREAKS.remove(event.player.getUniqueID());
    }

    /** クライアントのコンボと同じく、死亡（リスポーン）とディメンション移動で連続ヒットを最初に戻す。 */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        STREAKS.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        STREAKS.remove(event.player.getUniqueID());
    }

    /**
     * 受け付けたヒット（採掘・成長・機械・動物・釣りのすべて）を、連続ヒットに数えて、最大を更新する。
     * 種類とブロックをまたいで続き、40 tick ヒットがないと途切れる（HitStreak）。
     */
    public static void countStreak(EntityPlayerMP player) {
        long now = player.mcServer.getTickCounter();
        int count = STREAKS.computeIfAbsent(player.getUniqueID(), id -> new HitStreak()).hit(now);
        record(player, stats -> stats.recordStreak(count));
    }

    public static MiningStats session(EntityPlayer player) {
        return SESSIONS.computeIfAbsent(player.getUniqueID(), id -> new MiningStats());
    }

    public static MiningStats total(EntityPlayer player) {
        return read(data(player).getCompoundTag(TAG_TOTAL));
    }

    /** 「今回」と累計の両方に同じ記録をする。 */
    public static void record(EntityPlayer player, Consumer<MiningStats> change) {
        change.accept(session(player));
        MiningStats total = total(player);
        change.accept(total);
        data(player).setTag(TAG_TOTAL, write(total));
    }

    /** 節目に使う累計を1増やし、増えた後の値を返す。 */
    public static long addRewardHit(EntityPlayer player) {
        NBTTagCompound data = data(player);
        long hits = data.getLong(TAG_REWARD_HITS) + 1;
        data.setLong(TAG_REWARD_HITS, hits);
        return hits;
    }

    /** 画面の「累計をリセット」。 */
    public static void resetTotal(EntityPlayer player) {
        data(player).setTag(TAG_TOTAL, write(new MiningStats()));
    }

    /** この Mod の永続データ。無ければ作って付ける。 */
    static NBTTagCompound data(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        NBTTagCompound mine = persisted.getCompoundTag(TAG);
        persisted.setTag(TAG, mine);
        return mine;
    }

    private static MiningStats read(NBTTagCompound tag) {
        MiningStats stats = new MiningStats();
        stats.hits = tag.getLong("hits");
        stats.blocksBroken = tag.getLong("blocksBroken");
        stats.blocksBrokenWithHit = tag.getLong("blocksBrokenWithHit");
        stats.maxHitsOnBlock = tag.getLong("maxHitsOnBlock");
        stats.savedTicks = tag.getDouble("savedTicks");
        stats.growthHits = tag.getLong("growthHits");
        stats.machineHits = tag.getLong("machineHits");
        stats.maxStreak = tag.getLong("maxStreak");
        stats.animalHits = tag.getLong("animalHits");
        stats.fishingHits = tag.getLong("fishingHits");
        return stats;
    }

    private static NBTTagCompound write(MiningStats stats) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("hits", stats.hits);
        tag.setLong("blocksBroken", stats.blocksBroken);
        tag.setLong("blocksBrokenWithHit", stats.blocksBrokenWithHit);
        tag.setLong("maxHitsOnBlock", stats.maxHitsOnBlock);
        tag.setDouble("savedTicks", stats.savedTicks);
        tag.setLong("growthHits", stats.growthHits);
        tag.setLong("machineHits", stats.machineHits);
        tag.setLong("maxStreak", stats.maxStreak);
        tag.setLong("animalHits", stats.animalHits);
        tag.setLong("fishingHits", stats.fishingHits);
        return tag;
    }
}

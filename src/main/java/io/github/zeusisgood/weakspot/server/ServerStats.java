package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ComboMilestones;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.HitStreak;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.network.StatsMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
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

    private static final Map<UUID, MiningStats> SESSIONS = new HashMap<>();
    /** 連続ヒット（コンボ）。クライアントのコンボと同じ条件で、サーバーの tick で数える。メモリだけ。 */
    private static final Map<UUID, HitStreak> STREAKS = new HashMap<>();

    private ServerStats() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SESSIONS.put(event.player.getUniqueID(), new MiningStats());
    }

    /** クライアントのコンボと同じく、死亡（リスポーン）とディメンション移動で連続ヒットを最初に戻す。 */
    /** ログアウトで今回の統計と連続ヒットを、死亡・ディメンション移動で連続ヒットを消す（クライアントのコンボと同じ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            SESSIONS.remove(player.getUniqueID());
        }
        STREAKS.remove(player.getUniqueID());
    }

    /**
     * 受け付けたヒット（採掘・成長・機械・動物・釣り・弓・近接のすべて）を、連続ヒットに数えて、最大を更新する。
     * 種類とブロックをまたいで続き、40 tick ヒットがないと途切れる（HitStreak）。このヒットを数えたあとの数を返す。
     */
    public static int countStreak(EntityPlayerMP player) {
        long now = player.mcServer.getTickCounter();
        int count = STREAKS.computeIfAbsent(player.getUniqueID(), id -> new HitStreak()).hit(now);
        long bestBefore = total(player).maxStreak;
        record(player, stats -> stats.recordStreak(count));
        if (count > bestBefore && ComboMilestones.isStep(count)) {
            // 最大コンボを初めて超えて段階に届いたら、ほかの全員に知らせる（1.11.0）
            MiningRewards.broadcastCombo(player, count);
        }
        WeakSpotAdvancements.onHit(player, count);
        return count;
    }

    /**
     * このヒットを数えたら何になるか（数えない。1.7.0。収穫のおまけは、収穫できたときだけヒットに数えるので、先に見る）。
     */
    public static int peekStreak(EntityPlayerMP player) {
        HitStreak streak = STREAKS.get(player.getUniqueID());
        long now = player.mcServer.getTickCounter();
        return streak == null ? 1 : streak.count(now) + 1;
    }

    /** そのプレイヤーの連続ヒット（まだ一度もヒットしていなければ null）。頭の上のコンボ（ComboRelay）が読む。 */
    static HitStreak streak(EntityPlayer player) {
        return STREAKS.get(player.getUniqueID());
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

    /**
     * 採掘以外の種類のヒットを、「今回」と累計に数え、種類ごと・合計の節目を判定する（1.7.0）。
     * 近接はクリティカルにしたときに呼ぶ。
     */
    public static void recordKindHit(EntityPlayerMP player, HitKind kind) {
        record(player, stats -> stats.recordKindHit(kind));
        MiningRewards.onKindHit(player, kind);
    }

    /**
     * 画面の「統計をリセット」と /weakspot reset。累計と今回の両方を消す（1.10.2。それまでは累計だけで、直後に今回のほうが
     * 大きく見えた）。節目も累計で数えるので、節目をもう一度受け取れる（1.6.1）。
     */
    public static void resetTotal(EntityPlayer player) {
        // 的当ての記録は消さない（ご褒美の解放が消えてしまうため。1.11.0）
        MiningStats before = total(player);
        MiningStats fresh = new MiningStats();
        fresh.targetBest = before.targetBest;
        fresh.targetRounds = before.targetRounds;
        data(player).setTag(TAG_TOTAL, write(fresh));
        SESSIONS.put(player.getUniqueID(), new MiningStats());
    }

    /** 統計画面に送る中身（今回・累計と、的当てのサーバー内の上位 10 人。1.11.0）。 */
    public static StatsMessage message(EntityPlayerMP player) {
        List<String> names = new ArrayList<>();
        List<Long> scores = new ArrayList<>();
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (Leaderboard.Entry entry : Leaderboard.ranking(server, Leaderboard.Category.TARGET)) {
                if (names.size() >= TOP_SHOWN) {
                    break;
                }
                names.add(entry.name);
                scores.add(entry.value(Leaderboard.Category.TARGET));
            }
        }
        return new StatsMessage(session(player), total(player), names, scores);
    }

    /** 統計画面に出す、的当ての上位の人数。 */
    private static final int TOP_SHOWN = 10;

    /** この Mod の永続データ。無ければ作って付ける。 */
    static NBTTagCompound data(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        NBTTagCompound mine = persisted.getCompoundTag(TAG);
        // 1.6.1 でやめた節目の別の累計。1.9.0 から、読み込んだときに消す
        mine.removeTag("rewardHits");
        persisted.setTag(TAG, mine);
        return mine;
    }

    /** 保存のキーは MiningStats#saveKey。1.8.x で保存した近接の数（critHits）は、meleeHits がなければ読む（1.9.0）。 */
    private static MiningStats read(NBTTagCompound tag) {
        MiningStats stats = new MiningStats();
        stats.blocksBroken = tag.getLong("blocksBroken");
        stats.blocksBrokenWithHit = tag.getLong("blocksBrokenWithHit");
        // 1.10.0 から。1.9.x までのワールドにはないので 0 から数える
        stats.hitsOnBrokenBlocks = tag.getLong("hitsOnBrokenBlocks");
        stats.maxHitsOnBlock = tag.getLong("maxHitsOnBlock");
        stats.savedTicks = tag.getDouble("savedTicks");
        stats.maxStreak = tag.getLong("maxStreak");
        // 1.11.0 から
        stats.targetBest = tag.getLong("targetBest");
        stats.targetRounds = tag.getLong("targetRounds");
        for (HitKind kind : HitKind.values()) {
            String key = MiningStats.saveKey(kind);
            String legacy = MiningStats.legacySaveKey(kind);
            if (!tag.hasKey(key) && legacy != null) {
                key = legacy;
            }
            stats.setCount(kind, tag.getLong(key));
        }
        return stats;
    }

    private static NBTTagCompound write(MiningStats stats) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("blocksBroken", stats.blocksBroken);
        tag.setLong("blocksBrokenWithHit", stats.blocksBrokenWithHit);
        tag.setLong("hitsOnBrokenBlocks", stats.hitsOnBrokenBlocks);
        tag.setLong("maxHitsOnBlock", stats.maxHitsOnBlock);
        tag.setDouble("savedTicks", stats.savedTicks);
        tag.setLong("maxStreak", stats.maxStreak);
        tag.setLong("targetBest", stats.targetBest);
        tag.setLong("targetRounds", stats.targetRounds);
        for (HitKind kind : HitKind.values()) {
            tag.setLong(MiningStats.saveKey(kind), stats.count(kind));
        }
        return tag;
    }
}

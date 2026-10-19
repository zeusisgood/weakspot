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
 * 累計はプレイヤーの永続データ（PlayerPersisted。死亡・ディメンション移動でも引き継がれる）に保存するので、
 * ワールドごと・プレイヤーごとになる。「今回」はログインからログアウトまでで、メモリにだけ持つ。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ServerStats {

    private static final String TAG = WeakSpotMod.MODID;
    private static final String TAG_TOTAL = "total";

    private static final Map<UUID, MiningStats> SESSIONS = new HashMap<>();
    /** クライアントのコンボと同じ条件で、サーバーの tick で数える。メモリだけ。 */
    private static final Map<UUID, HitStreak> STREAKS = new HashMap<>();

    private ServerStats() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SESSIONS.put(event.player.getUniqueID(), new MiningStats());
    }

    /** リスポーン・ディメンション移動では連続ヒットだけを消す（クライアントのコンボと同じ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            SESSIONS.remove(player.getUniqueID());
        }
        STREAKS.remove(player.getUniqueID());
    }

    /** このヒットを数えたあとの数を返す。 */
    public static int countStreak(EntityPlayerMP player) {
        long now = player.mcServer.getTickCounter();
        int count = STREAKS.computeIfAbsent(player.getUniqueID(), id -> new HitStreak()).hit(now);
        long bestBefore = total(player).maxStreak;
        record(player, stats -> stats.recordStreak(count));
        if (count > bestBefore && ComboMilestones.isStep(count)) {
            // 最大コンボを初めて超えて段階に届いたら、ほかの全員に知らせる
            MiningRewards.broadcastCombo(player, count);
        }
        WeakSpotAdvancements.onHit(player, count);
        return count;
    }

    /** 数えない。収穫のおまけは、収穫できたときだけヒットに数えるので、先に見る。 */
    public static int peekStreak(EntityPlayerMP player) {
        HitStreak streak = STREAKS.get(player.getUniqueID());
        long now = player.mcServer.getTickCounter();
        return streak == null ? 1 : streak.count(now) + 1;
    }

    /** まだ一度もヒットしていなければ null。 */
    static HitStreak streak(EntityPlayer player) {
        return STREAKS.get(player.getUniqueID());
    }

    public static MiningStats session(EntityPlayer player) {
        return SESSIONS.computeIfAbsent(player.getUniqueID(), id -> new MiningStats());
    }

    public static MiningStats total(EntityPlayer player) {
        return read(data(player).getCompoundTag(TAG_TOTAL));
    }

    public static void record(EntityPlayer player, Consumer<MiningStats> change) {
        change.accept(session(player));
        MiningStats total = total(player);
        change.accept(total);
        data(player).setTag(TAG_TOTAL, write(total));
    }

    /** 近接はクリティカルにしたときに呼ぶ。 */
    public static void recordKindHit(EntityPlayerMP player, HitKind kind) {
        record(player, stats -> stats.recordKindHit(kind));
        MiningRewards.onKindHit(player, kind);
    }

    /** 累計と今回の両方を消す（累計だけだと、直後に今回のほうが大きく見える）。節目も累計で数えるので、もう一度受け取れる。 */
    public static void resetTotal(EntityPlayer player) {
        // 的当ての記録は消さない（ご褒美の解放が消えてしまうため）
        MiningStats before = total(player);
        MiningStats fresh = new MiningStats();
        fresh.targetBest = before.targetBest;
        fresh.targetRounds = before.targetRounds;
        data(player).setTag(TAG_TOTAL, write(fresh));
        SESSIONS.put(player.getUniqueID(), new MiningStats());
        if (player instanceof EntityPlayerMP) {
            // 最初からやり直す感じを出すため（的は配り直さない）
            GuideBookGiver.sendWelcome((EntityPlayerMP) player, false);
        }
    }

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

    private static final int TOP_SHOWN = 10;

    /** 無ければ作って付ける。 */
    static NBTTagCompound data(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        NBTTagCompound mine = persisted.getCompoundTag(TAG);
        // 古い版の、節目の別の累計（今は使わない）。読み込んだときに消す
        mine.removeTag("rewardHits");
        persisted.setTag(TAG, mine);
        return mine;
    }

    /** 古いワールドの近接の数（critHits）は、meleeHits がなければ読む。 */
    private static MiningStats read(NBTTagCompound tag) {
        MiningStats stats = new MiningStats();
        stats.blocksBroken = tag.getLong("blocksBroken");
        stats.blocksBrokenWithHit = tag.getLong("blocksBrokenWithHit");
        // 古いワールドにはないので 0 から数える
        stats.hitsOnBrokenBlocks = tag.getLong("hitsOnBrokenBlocks");
        stats.maxHitsOnBlock = tag.getLong("maxHitsOnBlock");
        stats.savedTicks = tag.getDouble("savedTicks");
        stats.maxStreak = tag.getLong("maxStreak");
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

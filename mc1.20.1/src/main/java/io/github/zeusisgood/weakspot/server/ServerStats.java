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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
        SESSIONS.put(event.getEntity().getUUID(), new MiningStats());
    }

    /** リスポーン・ディメンション移動では連続ヒットだけを消す（クライアントのコンボと同じ）。 */
    static void forget(Player player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            SESSIONS.remove(player.getUUID());
        }
        STREAKS.remove(player.getUUID());
    }

    /** このヒットを数えたあとの数を返す。 */
    public static int countStreak(ServerPlayer player) {
        long now = player.server.getTickCount();
        int count = STREAKS.computeIfAbsent(player.getUUID(), id -> new HitStreak()).hit(now);
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
    public static int peekStreak(ServerPlayer player) {
        HitStreak streak = STREAKS.get(player.getUUID());
        long now = player.server.getTickCount();
        return streak == null ? 1 : streak.count(now) + 1;
    }

    /** まだ一度もヒットしていなければ null。 */
    static HitStreak streak(Player player) {
        return STREAKS.get(player.getUUID());
    }

    public static MiningStats session(Player player) {
        return SESSIONS.computeIfAbsent(player.getUUID(), id -> new MiningStats());
    }

    public static MiningStats total(Player player) {
        return read(data(player).getCompound(TAG_TOTAL));
    }

    public static void record(Player player, Consumer<MiningStats> change) {
        change.accept(session(player));
        MiningStats total = total(player);
        change.accept(total);
        data(player).put(TAG_TOTAL, write(total));
    }

    /** 近接はクリティカルにしたときに呼ぶ。 */
    public static void recordKindHit(ServerPlayer player, HitKind kind) {
        record(player, stats -> stats.recordKindHit(kind));
        MiningRewards.onKindHit(player, kind);
    }

    /** 累計と今回の両方を消す（累計だけだと、直後に今回のほうが大きく見える）。節目も累計で数えるので、もう一度受け取れる。 */
    public static void resetTotal(Player player) {
        // 的当ての記録は消さない（ご褒美の解放が消えてしまうため）
        MiningStats before = total(player);
        MiningStats fresh = new MiningStats();
        fresh.targetBest = before.targetBest;
        fresh.targetRounds = before.targetRounds;
        data(player).put(TAG_TOTAL, write(fresh));
        SESSIONS.put(player.getUUID(), new MiningStats());
        if (player instanceof ServerPlayer serverPlayer) {
            // 最初からやり直す感じを出すため（的は配り直さない）
            GuideBookGiver.sendWelcome(serverPlayer, false);
        }
    }

    public static StatsMessage message(ServerPlayer player) {
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

    /** 無ければ作って付ける。中身を書き換えると保存される。 */
    static CompoundTag data(Player player) {
        CompoundTag entityData = player.getPersistentData();
        CompoundTag persisted = entityData.getCompound(Player.PERSISTED_NBT_TAG);
        entityData.put(Player.PERSISTED_NBT_TAG, persisted);
        CompoundTag mine = persisted.getCompound(TAG);
        persisted.put(TAG, mine);
        return mine;
    }

    /** 古いワールドの近接の数（critHits）は、meleeHits がなければ読む。 */
    private static MiningStats read(CompoundTag tag) {
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
            if (!tag.contains(key) && legacy != null) {
                key = legacy;
            }
            stats.setCount(kind, tag.getLong(key));
        }
        return stats;
    }

    private static CompoundTag write(MiningStats stats) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("blocksBroken", stats.blocksBroken);
        tag.putLong("blocksBrokenWithHit", stats.blocksBrokenWithHit);
        tag.putLong("hitsOnBrokenBlocks", stats.hitsOnBrokenBlocks);
        tag.putLong("maxHitsOnBlock", stats.maxHitsOnBlock);
        tag.putDouble("savedTicks", stats.savedTicks);
        tag.putLong("maxStreak", stats.maxStreak);
        tag.putLong("targetBest", stats.targetBest);
        tag.putLong("targetRounds", stats.targetRounds);
        for (HitKind kind : HitKind.values()) {
            tag.putLong(MiningStats.saveKey(kind), stats.count(kind));
        }
        return tag;
    }
}

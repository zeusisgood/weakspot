package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.network.OtherTargetMessage;
import io.github.zeusisgood.weakspot.network.TargetActionMessage;
import io.github.zeusisgood.weakspot.network.TargetMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 的当てのラウンド（サーバーが正）。ヒットはクライアントの知らせを、ラウンド中か・間隔で確かめて数える。
 * 的は使い捨て（始めた時点で 1 つ使う）なので、持ち替え・画面を開くではやめない。死亡・ディメンション移動・ログアウトでは
 * 記録なしでやめる。ラウンドのヒットは、いつものコンボ・統計・節目には入れない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class TargetRounds {

    private static final class Round {
        long start;
        int hits;
        /** 結果の板に出す。 */
        int good;
        int decoys;
        /** ✕ で 0 に戻す（近くの人のヒット音の音階）。 */
        int streak;
        long lastHit = Long.MIN_VALUE / 2;
    }

    private static final Map<UUID, Round> ROUNDS = new HashMap<>();
    /** ブロック。頭の上のコンボと同じ。 */
    private static final double OTHERS_RANGE = 32;

    private TargetRounds() {
    }

    private static long now(ServerPlayer player) {
        return player.server.getTickCount();
    }

    /**
    * ラウンドがなければ始めて true（呼ぶ側が的を 1 つ使う）。ラウンド中の右クリックは無視する
    * （押しっぱなしでも 4 tick ごとに届くため）。
    */
    public static boolean start(ServerPlayer player) {
        if (ROUNDS.containsKey(player.getUUID())) {
            return false;
        }
        Round round = new Round();
        round.start = now(player);
        ROUNDS.put(player.getUUID(), round);
        if (ServerStats.total(player).targetRounds == 0) {
            // 初めてのときは、始まる前にルールを伝える（ハズレのことを先に知らせる）
            player.sendSystemMessage(Component.translatable("weakspot.target.rules").withStyle(ChatFormatting.GOLD));
        }
        send(player, new TargetMessage(TargetMessage.START, 0, 0, false, -1));
        tellOthers(player, 0);
        return true;
    }

    public static void onAction(ServerPlayer player, byte action) {
        Round round = ROUNDS.get(player.getUUID());
        if (round == null) {
            return;
        }
        long now = now(player);
        long into = now - round.start - TargetRules.COUNTDOWN_TICKS;
        if (into < 0 || into >= TargetRules.ROUND_TICKS || now - round.lastHit < TargetRules.MIN_HIT_INTERVAL_TICKS - 2) {
            return;
        }
        round.lastHit = now;
        boolean decoy = action == TargetActionMessage.DECOY;
        round.hits = TargetRules.afterHit(round.hits, decoy);
        if (decoy) {
            round.decoys++;
            round.streak = 0;
        } else {
            round.good++;
            round.streak++;
        }
        // 位置は遊んでいる人
        ServerBoostTracker.notifyNearbyPlayers(player, player.blockPosition(), round.streak, decoy);
        send(player, new TargetMessage(TargetMessage.SCORE, round.hits, 0, false, -1));
        tellOthers(player, round.hits);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (ROUNDS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        List<UUID> ids = new ArrayList<>(ROUNDS.keySet());
        for (UUID id : ids) {
            Round round = ROUNDS.get(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                ROUNDS.remove(id);
                continue;
            }
            if (now(player) - round.start >= TargetRules.COUNTDOWN_TICKS + TargetRules.ROUND_TICKS) {
                finish(player, round, server);
            }
        }
    }

    private static void finish(ServerPlayer player, Round round, MinecraftServer server) {
        ROUNDS.remove(player.getUUID());
        tellOthers(player, -1);
        long previousTop = Leaderboard.best(server, Leaderboard.Category.TARGET);
        MiningStats before = ServerStats.total(player);
        // しきい値を上げる前に取った段階（進捗）も、もう届いている段階として数える
        TargetRules.Tier earned = WeakSpotAdvancements.earnedTargetTier(player);
        TargetRules.Tier oldTier = TargetRules.Tier.of(before.targetBest);
        if (earned.atLeast(oldTier)) {
            oldTier = earned;
        }
        int hits = round.hits;
        ServerStats.record(player, stats -> stats.recordTargetRound(hits));
        long best = Math.max(before.targetBest, hits);
        boolean newBest = hits > before.targetBest;
        TargetRules.Tier tier = TargetRules.Tier.of(best);
        Leaderboard.update(player);
        List<Leaderboard.Entry> ranking = Leaderboard.ranking(server, Leaderboard.Category.TARGET);
        Leaderboard.Entry top = ranking.isEmpty() ? null : ranking.get(0);
        send(player, new TargetMessage(TargetMessage.END, hits, best, newBest,
                tier.ordinal() > oldTier.ordinal() ? tier.ordinal() : -1)
                .withResult(round.good, round.decoys, top == null ? "" : top.name,
                        top == null ? 0 : top.value(Leaderboard.Category.TARGET)));
        WeakSpotAdvancements.onTargetBest(player, best);
        if (hits > previousTop) {
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                other.sendSystemMessage(Component.translatable("weakspot.milestone.broadcast.target",
                        player.getName(), hits).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** 取った進捗のいちばん上の段階は、RECORD の newTier の欄で送る（しきい値を上げる前に解放した段階を残すため）。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player, new TargetMessage(TargetMessage.RECORD, 0, ServerStats.total(player).targetBest, false,
                    WeakSpotAdvancements.earnedTargetTier(player).ordinal()));
        }
    }

    static void forget(Player player, HitGate.Leave leave) {
        if (ROUNDS.remove(player.getUUID()) != null && player instanceof ServerPlayer serverPlayer) {
            tellOthers(serverPlayer, -1);
            if (leave != HitGate.Leave.LOGOUT) {
                send(serverPlayer, new TargetMessage(TargetMessage.CANCEL, 0, 0, false, -1));
            }
        }
    }

    private static void send(ServerPlayer player, TargetMessage message) {
        WeakSpotMod.network.sendTo(message, player);
    }

    /** hits が負なら終わった。 */
    private static void tellOthers(ServerPlayer player, int hits) {
        OtherTargetMessage message = new OtherTargetMessage(player.getId(), hits);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && other.distanceToSqr(player) <= OTHERS_RANGE * OTHERS_RANGE) {
                WeakSpotMod.network.sendTo(message, other);
            }
        }
    }
}

package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.ItemTarget;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.network.TargetActionMessage;
import io.github.zeusisgood.weakspot.network.TargetMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 的当てのラウンド（1.11.0。サーバーが正）。「弱点の的」の右クリックで始め（カウントダウン 3 秒 + 30 秒）、ヒットは
 * クライアントの知らせ（TargetActionMessage）を、ラウンド中か・間隔で確かめて数える（当たり +1、ハズレ −5、0 より下に
 * ならない）。終わったら記録（MiningStats の targetBest・targetRounds。「統計をリセット」では消さない）を残し、
 * サーバーの 1 位を上回ったら全員に知らせる。持ち替え・もう一度の右クリック・画面を開く（クライアントから）・死亡・
 * ディメンション移動・ログアウトでやめる（記録なし）。ラウンドのヒットは、いつものコンボ・統計・節目には入れない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class TargetRounds {

    /** 翻訳キー weakspot.target.* と weakspot.milestone.broadcast.target を足した版。 */
    static final String SINCE = "1.11.0";

    private static final class Round {
        long start;
        int hits;
        long lastHit = Long.MIN_VALUE / 2;
        EnumHand hand;
        int slot;
    }

    private static final Map<UUID, Round> ROUNDS = new HashMap<>();

    private TargetRounds() {
    }

    private static long now(EntityPlayerMP player) {
        return player.mcServer.getTickCounter();
    }

    /** 「弱点の的」の右クリック。ラウンドがなければ始め、あればやめる（始めた直後の押しっぱなしは除く）。 */
    public static void toggle(EntityPlayerMP player, EnumHand hand) {
        Round round = ROUNDS.get(player.getUniqueID());
        long now = now(player);
        if (round != null) {
            if (now - round.start >= TargetRules.CANCEL_GUARD_TICKS) {
                cancel(player, true);
            }
            return;
        }
        round = new Round();
        round.start = now;
        round.hand = hand;
        round.slot = player.inventory.currentItem;
        ROUNDS.put(player.getUniqueID(), round);
        if (ServerStats.total(player).targetRounds == 0) {
            // 初めてのときは、始まる前にルールを伝える（ハズレのことを先に知らせる）
            ITextComponent rules = PlayerText.of(player, SINCE, "weakspot.target.rules");
            rules.getStyle().setColor(TextFormatting.GOLD);
            player.sendMessage(rules);
        }
        send(player, new TargetMessage(TargetMessage.START, 0, 0, false, -1));
    }

    /** クライアントからの知らせ（サーバースレッド）。 */
    public static void onAction(EntityPlayerMP player, byte action) {
        Round round = ROUNDS.get(player.getUniqueID());
        if (round == null) {
            return;
        }
        if (action == TargetActionMessage.CANCEL) {
            cancel(player, false);
            return;
        }
        long now = now(player);
        long into = now - round.start - TargetRules.COUNTDOWN_TICKS;
        if (into < 0 || into >= TargetRules.ROUND_TICKS || now - round.lastHit < TargetRules.MIN_HIT_INTERVAL_TICKS - 2) {
            return;
        }
        round.lastHit = now;
        round.hits = TargetRules.afterHit(round.hits, action == TargetActionMessage.DECOY);
        send(player, new TargetMessage(TargetMessage.SCORE, round.hits, 0, false, -1));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ROUNDS.isEmpty()) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        List<UUID> ids = new ArrayList<>(ROUNDS.keySet());
        for (UUID id : ids) {
            Round round = ROUNDS.get(id);
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player == null) {
                ROUNDS.remove(id);
                continue;
            }
            boolean holding = player.getHeldItem(round.hand).getItem() == ItemTarget.INSTANCE
                    && (round.hand != EnumHand.MAIN_HAND || player.inventory.currentItem == round.slot);
            if (!holding) {
                cancel(player, true);
            } else if (now(player) - round.start >= TargetRules.COUNTDOWN_TICKS + TargetRules.ROUND_TICKS) {
                finish(player, round, server);
            }
        }
    }

    /** 最後まで遊んだ。記録を残し、自己ベスト・新しいご褒美の段階・サーバーの 1 位を知らせる。 */
    private static void finish(EntityPlayerMP player, Round round, MinecraftServer server) {
        ROUNDS.remove(player.getUniqueID());
        long previousTop = Leaderboard.best(server, Leaderboard.Category.TARGET);
        MiningStats before = ServerStats.total(player);
        TargetRules.Tier oldTier = TargetRules.Tier.of(before.targetBest);
        int hits = round.hits;
        ServerStats.record(player, stats -> stats.recordTargetRound(hits));
        long best = Math.max(before.targetBest, hits);
        boolean newBest = hits > before.targetBest;
        TargetRules.Tier tier = TargetRules.Tier.of(best);
        Leaderboard.update(player);
        send(player, new TargetMessage(TargetMessage.END, hits, best, newBest,
                tier != oldTier ? tier.ordinal() : -1));
        WeakSpotAdvancements.onTargetBest(player, best);
        if (hits > previousTop) {
            for (EntityPlayerMP other : server.getPlayerList().getPlayers()) {
                ITextComponent text = PlayerText.of(other, SINCE, "weakspot.milestone.broadcast.target",
                        player.getName(), hits);
                text.getStyle().setColor(TextFormatting.GOLD);
                other.sendMessage(text);
            }
        }
    }

    private static void cancel(EntityPlayerMP player, boolean notify) {
        if (ROUNDS.remove(player.getUniqueID()) != null && notify) {
            send(player, new TargetMessage(TargetMessage.CANCEL, 0, 0, false, -1));
        }
    }

    /** ログイン: ご褒美の解放のため、自己ベストを送る。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            send(player, new TargetMessage(TargetMessage.RECORD, 0, ServerStats.total(player).targetBest, false, -1));
        }
    }

    /** 死亡・ディメンション移動・ログアウトで、ラウンドをやめる（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (ROUNDS.remove(player.getUniqueID()) != null && leave != HitGate.Leave.LOGOUT
                && player instanceof EntityPlayerMP) {
            send((EntityPlayerMP) player, new TargetMessage(TargetMessage.CANCEL, 0, 0, false, -1));
        }
    }

    private static void send(EntityPlayerMP player, TargetMessage message) {
        WeakSpotMod.network.sendTo(message, player);
    }
}

package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ComboSync;
import io.github.zeusisgood.weakspot.common.HitStreak;
import io.github.zeusisgood.weakspot.network.OtherComboMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * プレイヤーのコンボ（サーバーで数えている連続ヒット数）を、近くのほかのプレイヤーに送る（1.6.0。頭の上の「n HIT」）。
 * 数が変わったとき（途切れたときは 0）に、RANGE ブロック以内のほかのプレイヤーへ送る。1 人あたり MIN_INTERVAL_TICKS に
 * 1 回までに間引き、間引いた分は次に送れるときに送る。弱点を一時オフにしているプレイヤーの数は送らない。
 * <p>
 * 1.11.3 から、本人にも送る（クライアントとサーバーのコンボを揃える。ComboSync）。ヒットのたびにクライアントが送ってきた数と
 * 比べ（checkOwn）、ずれていたら、最後のヒットから ComboSync.IDLE_TICKS たったときに今のサーバーの数を送る。
 * 送る相手は 1.11.3 以降のクライアントだけ（古いクライアントは、自分の ID を「ほかのプレイヤー」として描いてしまう）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ComboRelay {

    static final double RANGE = 32;
    static final int MIN_INTERVAL_TICKS = 4;

    private static final Map<UUID, Sent> SENT = new HashMap<>();
    /** 本人宛て（1.11.3）。 */
    private static final Map<UUID, Own> OWN = new HashMap<>();
    /** 本人宛てを送る相手の版。 */
    private static final String OWN_SINCE = "1.11.3";

    private static final class Sent {
        int count;
        long tick = Long.MIN_VALUE / 2;
    }

    private static final class Own {
        /** 最後のヒットで、クライアントの数がサーバーとずれていたか。 */
        boolean mismatched;
        long lastHitTick;
        long sentTick = Long.MIN_VALUE / 2;
    }

    private ComboRelay() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        long now = server.getTickCounter();
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            HitStreak streak = ServerStats.streak(player);
            int count = streak == null || !ServerSwitches.isEnabled(player) ? 0 : streak.count(now);
            Sent sent = SENT.computeIfAbsent(player.getUniqueID(), id -> new Sent());
            if (count == sent.count || now - sent.tick < MIN_INTERVAL_TICKS) {
                continue;
            }
            sent.count = count;
            sent.tick = now;
            OtherComboMessage message = new OtherComboMessage(player.getEntityId(), count);
            for (EntityPlayer other : player.world.playerEntities) {
                if (other != player && other instanceof EntityPlayerMP
                        && other.getDistanceSq(player) <= RANGE * RANGE) {
                    WeakSpotMod.network.sendTo(message, (EntityPlayerMP) other);
                }
            }
        }
        sendOwn(server, now);
        for (Iterator<UUID> it = SENT.keySet().iterator(); it.hasNext(); ) {
            if (server.getPlayerList().getPlayerByUUID(it.next()) == null) {
                it.remove();
            }
        }
        OWN.keySet().removeIf(id -> server.getPlayerList().getPlayerByUUID(id) == null);
    }

    /**
     * クライアントからのヒットを処理したあとに呼ぶ（HitHandlers。1.11.3）。clientStreak はクライアントのヒット後の数。
     * 比べるだけで、サーバーの数をこれに合わせることはしない。
     */
    static void checkOwn(EntityPlayerMP player, int clientStreak) {
        Own own = OWN.computeIfAbsent(player.getUniqueID(), id -> new Own());
        own.lastHitTick = player.mcServer.getTickCounter();
        own.mismatched = ComboSync.mismatched(clientStreak, serverCount(player, own.lastHitTick),
                ServerBoostTracker.hasReservation(player));
    }

    /** ずれていたプレイヤーに、手が止まったら今のサーバーの数を送る。 */
    private static void sendOwn(MinecraftServer server, long now) {
        for (Map.Entry<UUID, Own> entry : OWN.entrySet()) {
            Own own = entry.getValue();
            if (!own.mismatched || !ComboSync.canSend(now, own.lastHitTick, own.sentTick)) {
                continue;
            }
            own.mismatched = false;
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
            if (player == null || !PlayerText.clientSince(player, OWN_SINCE)) {
                continue;
            }
            own.sentTick = now;
            int count = ComboSync.expected(serverCount(player, now), ServerBoostTracker.hasReservation(player));
            WeakSpotMod.network.sendTo(new OtherComboMessage(player.getEntityId(), count), player);
        }
    }

    private static int serverCount(EntityPlayer player, long now) {
        HitStreak streak = ServerStats.streak(player);
        return streak == null ? 0 : streak.count(now);
    }

    /** 後片付け（HitGate から呼ぶ。1.8.9 でログアウト、1.11.3 から本人宛ては死亡・ディメンション移動も。両側とも 0 に戻すため）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            SENT.remove(player.getUniqueID());
        }
        OWN.remove(player.getUniqueID());
    }
}

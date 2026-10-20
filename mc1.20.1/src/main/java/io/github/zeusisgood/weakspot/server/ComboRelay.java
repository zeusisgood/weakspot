package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ComboSync;
import io.github.zeusisgood.weakspot.common.HitStreak;
import io.github.zeusisgood.weakspot.network.OtherComboMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 頭の上の「n HIT」。1 人あたり MIN_INTERVAL_TICKS に 1 回までに間引き、間引いた分は次に送れるときに送る。
 * 弱点を一時オフにしているプレイヤーの数は送らない。
 * <p>
 * 本人にも送る（クライアントとサーバーのコンボを揃える。ComboSync）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ComboRelay {

    static final double RANGE = 32;
    static final int MIN_INTERVAL_TICKS = 4;

    private static final Map<UUID, Sent> SENT = new HashMap<>();
    /** 本人宛て。 */
    private static final Map<UUID, Own> OWN = new HashMap<>();

    private static final class Sent {
        int count;
        long tick = Long.MIN_VALUE / 2;
    }

    private static final class Own {
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
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        long now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HitStreak streak = ServerStats.streak(player);
            int count = streak == null || !ServerSwitches.isEnabled(player) ? 0 : streak.count(now);
            Sent sent = SENT.computeIfAbsent(player.getUUID(), id -> new Sent());
            if (count == sent.count || now - sent.tick < MIN_INTERVAL_TICKS) {
                continue;
            }
            sent.count = count;
            sent.tick = now;
            OtherComboMessage message = new OtherComboMessage(player.getId(), count);
            for (Player other : player.level().players()) {
                if (other != player && other instanceof ServerPlayer
                        && other.distanceToSqr(player) <= RANGE * RANGE) {
                    WeakSpotMod.network.sendTo(message, (ServerPlayer) other);
                }
            }
        }
        sendOwn(server, now);
        for (Iterator<UUID> it = SENT.keySet().iterator(); it.hasNext(); ) {
            if (server.getPlayerList().getPlayer(it.next()) == null) {
                it.remove();
            }
        }
        OWN.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    /** clientStreak はクライアントのヒット後の数。比べるだけで、サーバーの数をこれに合わせることはしない。 */
    static void checkOwn(ServerPlayer player, int clientStreak) {
        Own own = OWN.computeIfAbsent(player.getUUID(), id -> new Own());
        own.lastHitTick = player.server.getTickCount();
        own.mismatched = ComboSync.mismatched(clientStreak, serverCount(player, own.lastHitTick),
                ServerBoostTracker.hasReservation(player));
    }

    /** 手が止まったら送る。 */
    private static void sendOwn(MinecraftServer server, long now) {
        for (Map.Entry<UUID, Own> entry : OWN.entrySet()) {
            Own own = entry.getValue();
            if (!own.mismatched || !ComboSync.canSend(now, own.lastHitTick, own.sentTick)) {
                continue;
            }
            own.mismatched = false;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            own.sentTick = now;
            int count = ComboSync.expected(serverCount(player, now), ServerBoostTracker.hasReservation(player));
            WeakSpotMod.network.sendTo(new OtherComboMessage(player.getId(), count), player);
        }
    }

    private static int serverCount(Player player, long now) {
        HitStreak streak = ServerStats.streak(player);
        return streak == null ? 0 : streak.count(now);
    }

    /** 本人宛ては死亡・ディメンション移動でも消す（両側とも 0 に戻すため）。 */
    static void forget(Player player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            SENT.remove(player.getUUID());
        }
        OWN.remove(player.getUUID());
    }
}

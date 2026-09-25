package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitStreak;
import com.example.weakspot.network.OtherComboMessage;
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
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * プレイヤーのコンボ（サーバーで数えている連続ヒット数）を、近くのほかのプレイヤーに送る（1.6.0。頭の上の「n HIT」）。
 * 数が変わったとき（途切れたときは 0）に、RANGE ブロック以内のほかのプレイヤーへ送る。1 人あたり MIN_INTERVAL_TICKS に
 * 1 回までに間引き、間引いた分は次に送れるときに送る。弱点を一時オフにしているプレイヤーの数は送らない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ComboRelay {

    static final double RANGE = 32;
    static final int MIN_INTERVAL_TICKS = 4;

    private static final Map<UUID, Sent> SENT = new HashMap<>();

    private static final class Sent {
        int count;
        long tick = Long.MIN_VALUE / 2;
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
        for (Iterator<UUID> it = SENT.keySet().iterator(); it.hasNext(); ) {
            if (server.getPlayerList().getPlayerByUUID(it.next()) == null) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.player.getUniqueID());
    }
}

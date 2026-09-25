package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.SleepTime;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 寝ている間の弱点のヒット通知の検証と効果（論理サーバー。1.6.0）。夜にベッドで寝ているプレイヤーのヒットを受け付け、
 * ワールドの時刻を sleepHitTicks 進める（次の朝を越えない。時計の値だけで、作物などは早く動かない）。
 * 朝になれば、バニラどおり起きる。時刻はワールドの全員で共通なので、ほかのプレイヤーの時間も進む。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class SleepHits {

    private static final int INTERVAL_JITTER_TICKS = 2;

    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

    private SleepHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        World world = player.world;
        if (!ServerSwitches.isEnabled(player, HitKind.SLEEP) || player.isSpectator() || !WeakSpotConfig.sleepWeakSpotEnabled
                || !player.isPlayerSleeping() || !world.getGameRules().getBoolean("doDaylightCycle")
                || !SleepTime.isNight(world.getWorldTime())) {
            return;
        }
        long now = world.getTotalWorldTime();
        Long last = LAST_HIT.get(player.getUniqueID());
        int minInterval = Math.max(0, WeakSpotConfig.sleepMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (last != null && now - last < minInterval) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        world.setWorldTime(SleepTime.advance(world.getWorldTime(), WeakSpotConfig.sleepHitTicks));
        // 20 tick ごとの時刻の送信を待たずに、すぐに知らせる（空の明るさがすぐに変わるように）
        player.mcServer.getPlayerList().sendPacketToAllPlayersInDimension(new SPacketTimeUpdate(
                world.getTotalWorldTime(), world.getWorldTime(), true), world.provider.getDimension());
        ServerStats.recordKindHit(player, HitKind.SLEEP);
        ServerStats.countStreak(player);
        BlockPos bed = player.bedLocation != null ? player.bedLocation : new BlockPos(player);
        ServerBoostTracker.notifyNearbyPlayers(player, bed, streak);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.player.getUniqueID());
    }
}

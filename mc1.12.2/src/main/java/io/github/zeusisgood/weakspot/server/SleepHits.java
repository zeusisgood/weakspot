package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.SleepTime;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** 時刻はワールドの全員で共通なので、ほかのプレイヤーの時間も進む。時計の値だけで、作物などは早く動かない。 */
public final class SleepHits {

    private SleepHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        World world = player.world;
        if (!HitGate.allowed(player, HitKind.SLEEP)
                || !player.isPlayerSleeping() || !world.getGameRules().getBoolean("doDaylightCycle")
                || !SleepTime.isNight(world.getWorldTime()) || !HitGate.ready(player, HitKind.SLEEP)) {
            return;
        }
        world.setWorldTime(SleepTime.advance(world.getWorldTime(), WeakSpotConfig.server.sleep.sleepHitTicks));
        // 20 tick ごとの時刻の送信を待たずに、すぐに知らせる（空の明るさがすぐに変わるように）
        player.mcServer.getPlayerList().sendPacketToAllPlayersInDimension(new SPacketTimeUpdate(
                world.getTotalWorldTime(), world.getWorldTime(), true), world.provider.getDimension());
        BlockPos bed = player.bedLocation != null ? player.bedLocation : new BlockPos(player);
        HitGate.accept(player, HitKind.SLEEP, bed, streak);
    }

}

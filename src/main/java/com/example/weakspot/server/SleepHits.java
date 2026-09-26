package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.SleepTime;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;

/**
 * 寝ている間の弱点のヒット通知の検証と効果（論理サーバー。1.6.0）。夜にベッドで寝ているプレイヤーのヒットを受け付け、
 * ワールドの時刻を sleepHitTicks 進める（次の朝を越えない。時計の値だけで、作物などは早く動かない）。
 * 朝になれば、バニラどおり起きる。時刻はワールドの全員で共通なので、ほかのプレイヤーの時間も進む。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class SleepHits {

    private SleepHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        World world = player.world;
        if (!HitGate.allowed(player, HitKind.SLEEP) || !WeakSpotConfig.sleepWeakSpotEnabled
                || !player.isPlayerSleeping() || !world.getGameRules().getBoolean("doDaylightCycle")
                || !SleepTime.isNight(world.getWorldTime())) {
            return;
        }
        if (!HitGate.ready(player, HitKind.SLEEP, WeakSpotConfig.sleepMinHitIntervalTicks)) {
            return;
        }
        world.setWorldTime(SleepTime.advance(world.getWorldTime(), WeakSpotConfig.sleepHitTicks));
        // 20 tick ごとの時刻の送信を待たずに、すぐに知らせる（空の明るさがすぐに変わるように）
        player.mcServer.getPlayerList().sendPacketToAllPlayersInDimension(new SPacketTimeUpdate(
                world.getTotalWorldTime(), world.getWorldTime(), true), world.provider.getDimension());
        BlockPos bed = player.bedLocation != null ? player.bedLocation : new BlockPos(player);
        HitGate.accept(player, HitKind.SLEEP, bed, streak);
    }

}

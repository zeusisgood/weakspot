package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.SleepTime;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

/** 時刻はワールドの全員で共通なので、ほかのプレイヤーの時間も進む。時計の値だけで、作物などは早く動かない。 */
public final class SleepHits {

    private SleepHits() {
    }

    public static void onHit(ServerPlayer player, int streak) {
        ServerLevel world = player.serverLevel();
        if (!HitGate.allowed(player, HitKind.SLEEP)
                || !player.isSleeping() || !world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)
                || !SleepTime.isNight(world.getDayTime()) || !HitGate.ready(player, HitKind.SLEEP)) {
            return;
        }
        world.setDayTime(SleepTime.advance(world.getDayTime(), WeakSpotConfig.server.sleep.sleepHitTicks));
        // 20 tick ごとの時刻の送信を待たずに、すぐに知らせる（空の明るさがすぐに変わるように）
        player.server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(
                world.getGameTime(), world.getDayTime(), true), world.dimension());
        HitGate.accept(player, HitKind.SLEEP, player.getSleepingPos().orElse(player.blockPosition()), streak);
    }
}

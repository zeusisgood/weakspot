package com.example.weakspot.server;

import com.example.weakspot.BowDraw;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BowMath;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 弓の弱点のヒット通知の検証と効果（論理サーバー）。弓を引いている最中に受け付け、引き切る前は弓の引きを
 * bowHitTicks 進め、引き切ったあとは過剰チャージ（矢のダメージ +10%、上限 5 回）を 1 増やす（BowDraw）。照準の角度は確かめない（クライアントを信用する。釣りと同じ程度の確認）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class BowHits {

    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 2;

    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

    private BowHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!ServerSwitches.isEnabled(player) || player.capabilities.isCreativeMode || player.isSpectator()
                || !WeakSpotConfig.bowWeakSpotEnabled || WeakSpotConfig.bowHitTicks <= 0) {
            return;
        }
        if (!BowDraw.isDrawing(player)) {
            return;
        }
        // 引き切ったあとは、過剰チャージ（上限まで）
        boolean full = BowMath.isFull(BowDraw.usedTicks(player));
        if (full && !BowMath.canOvercharge(BowDraw.overcharge(player))) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_HIT.get(player.getUniqueID());
        int minInterval = Math.max(0, WeakSpotConfig.bowMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (last != null && now - last < minInterval) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        if (full) {
            BowDraw.addOvercharge(player);
        } else {
            BowDraw.add(player, WeakSpotConfig.bowHitTicks);
        }
        ServerStats.record(player, stats -> stats.recordBowHit());
        int combo = ServerStats.countStreak(player);
        // 乗り物に乗っていれば、加速も続ける（騎射。1.6.0）
        VehicleHits.boostFromRider(player, combo);
        ServerBoostTracker.notifyNearbyPlayers(player, new BlockPos(player), streak);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.player.getUniqueID());
    }
}

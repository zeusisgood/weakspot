package com.example.weakspot.server;

import com.example.weakspot.EatDraw;
import com.example.weakspot.WeakSpotMod;
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
 * 食事・飲み物の弱点のヒット通知の検証と効果（論理サーバー。1.6.0）。食べている・飲んでいる最中に受け付け、
 * 食べ終わるまでの時間を eatHitTicks 縮める（EatDraw）。乗り物に乗っていれば、加速も続ける。照準の角度は確かめない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class EatHits {

    private static final int INTERVAL_JITTER_TICKS = 2;

    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

    private EatHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!ServerSwitches.isEnabled(player) || player.capabilities.isCreativeMode || player.isSpectator()
                || !WeakSpotConfig.eatWeakSpotEnabled || WeakSpotConfig.eatHitTicks <= 0
                || !EatDraw.isEating(player)) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_HIT.get(player.getUniqueID());
        int minInterval = Math.max(0, WeakSpotConfig.eatMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (last != null && now - last < minInterval) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        EatDraw.add(player, WeakSpotConfig.eatHitTicks);
        ServerStats.record(player, stats -> stats.recordEatHit());
        int combo = ServerStats.countStreak(player);
        VehicleHits.boostFromRider(player, combo);
        ServerBoostTracker.notifyNearbyPlayers(player, new BlockPos(player), streak);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.player.getUniqueID());
    }
}

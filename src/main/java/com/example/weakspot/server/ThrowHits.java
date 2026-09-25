package com.example.weakspot.server;

import com.example.weakspot.ThrowCharge;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MachineComboBoost;
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
 * 投げる物の弱点のヒット通知の検証と効果（論理サーバー。1.7.0）。メインハンドに投げる物を持っている間に受け付け、
 * 次の 1 投の溜めを throwChargePerHit × コンボの掛け数だけ増やす（ThrowCharge。上限なし）。乗っていれば、乗り物の
 * 加速も続ける（騎射と同じ）。照準の角度は確かめない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ThrowHits {

    private static final int INTERVAL_JITTER_TICKS = 2;

    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

    private ThrowHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!ServerSwitches.isEnabled(player, HitKind.THROW) || player.capabilities.isCreativeMode
                || player.isSpectator() || !WeakSpotConfig.throwWeakSpotEnabled
                || !ThrowCharge.isHoldingThrowable(player) || player.isHandActive()) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_HIT.get(player.getUniqueID());
        int minInterval = Math.max(0, WeakSpotConfig.throwMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (last != null && now - last < minInterval) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        int combo = ServerStats.countStreak(player);
        ThrowCharge.add(player, WeakSpotConfig.throwChargePerHit * MachineComboBoost.factor(combo));
        ServerStats.recordKindHit(player, HitKind.THROW);
        VehicleHits.boostFromRider(player, combo);
        ServerBoostTracker.notifyNearbyPlayers(player, new BlockPos(player), streak);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.player.getUniqueID());
        ThrowCharge.clear(event.player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ThrowCharge.clear(event.player);
    }
}

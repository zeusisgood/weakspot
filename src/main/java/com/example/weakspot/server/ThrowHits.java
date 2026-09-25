package com.example.weakspot.server;

import com.example.weakspot.ThrowCharge;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
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



    private ThrowHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.THROW) || !WeakSpotConfig.throwWeakSpotEnabled
                || !ThrowCharge.isHoldingThrowable(player) || player.isHandActive()) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        if (!HitGate.ready(player, HitKind.THROW, WeakSpotConfig.throwMinHitIntervalTicks)) {
            return;
        }
        HitGate.mark(player, HitKind.THROW);
        int combo = ServerStats.countStreak(player);
        ThrowCharge.add(player, WeakSpotConfig.throwChargePerHit * ComboFactor.factor(combo));
        ServerStats.recordKindHit(player, HitKind.THROW);
        VehicleHits.boostFromRider(player, combo);
        ServerBoostTracker.notifyNearbyPlayers(player, new BlockPos(player), streak);
    }


    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ThrowCharge.clear(event.player);
    }

    /** ログアウトの後片付け（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player) {
        ThrowCharge.clear(player);
    }
}

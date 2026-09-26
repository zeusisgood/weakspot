package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.EatDraw;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;

/**
 * 食事・飲み物の弱点のヒット通知の検証と効果（論理サーバー。1.6.0）。食べている・飲んでいる最中に受け付け、
 * 食べ終わるまでの時間を eatHitTicks 縮める（EatDraw）。乗り物に乗っていれば、加速も続ける。照準の角度は確かめない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class EatHits {

    private EatHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.EAT) || WeakSpotConfig.eatHitTicks <= 0
                || !EatDraw.isEating(player)) {
            return;
        }
        if (!HitGate.ready(player, HitKind.EAT)) {
            return;
        }
        EatDraw.add(player, WeakSpotConfig.eatHitTicks);
        int combo = HitGate.accept(player, HitKind.EAT, new BlockPos(player), streak);
        VehicleHits.boostFromRider(player, combo);
    }

}

package com.example.weakspot.client;

import com.example.weakspot.EatDraw;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * 食事・飲み物の弱点（自分だけ。1.6.0）。食べている・飲んでいる間、照準から離れた所（馬・豚に乗っているときは真上か
 * 真下だけ）に緑の弱点を出し、照準を合わせるだけでヒットにする。当てると食べ終わるまでの時間が縮む（EatDraw。
 * クライアントもサーバーの返事を待たずに縮める）。乗っていれば、乗り物の加速も続ける。
 * 1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class EatSpot extends AimSpotKind {

    /** 緑 #7CFC00。 */
    private static final int RGB = 0x7CFC00;

    EatSpot() {
        super(HitKind.EAT, new HudSpot(HitKind.EAT, RGB));
    }

    @Override
    boolean blockedByUsingHand() {
        return false;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return settings.eatHitTicks > 0 && EatDraw.isEating(player);
    }

    @Override
    int placement(EntityPlayerSP player) {
        return VehicleTargets.isSteeredByLook(player) ? HudSpot.VERTICAL : HudSpot.FREE;
    }


    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        EatDraw.add(player, settings.eatHitTicks);
        VehicleSpot.onRiderHit(streak);
    }
}

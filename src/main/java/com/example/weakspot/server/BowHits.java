package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.BowDraw;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BowMath;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;

/**
 * 弓の弱点のヒット通知の検証と効果（論理サーバー）。弓を引いている最中に受け付け、引き切る前は弓の引きを
 * bowHitTicks 進め、引き切ったあとは過剰チャージ（矢のダメージ +10%、上限 5 回）を 1 増やす（BowDraw）。照準の角度は確かめない（クライアントを信用する。釣りと同じ程度の確認）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class BowHits {

    private BowHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.BOW)
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
        if (!HitGate.ready(player, HitKind.BOW, WeakSpotConfig.bowMinHitIntervalTicks)) {
            return;
        }
        if (full) {
            BowDraw.addOvercharge(player);
        } else {
            BowDraw.add(player, WeakSpotConfig.bowHitTicks);
        }
        int combo = HitGate.accept(player, HitKind.BOW, new BlockPos(player), streak);
        // 乗り物に乗っていれば、加速も続ける（騎射。1.6.0）
        VehicleHits.boostFromRider(player, combo);
    }

}

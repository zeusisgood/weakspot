package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.BowDraw;
import io.github.zeusisgood.weakspot.common.BowMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.server.level.ServerPlayer;

/** 照準の角度は確かめない（クライアントを信用する。釣りと同じ程度の確認）。 */
public final class BowHits {

    private BowHits() {
    }

    public static void onHit(ServerPlayer player, int streak) {
        if (!HitGate.allowed(player, HitKind.BOW) || WeakSpotConfig.server.bow.bowHitTicks <= 0) {
            return;
        }
        if (!BowDraw.isDrawing(player)) {
            return;
        }
        boolean full = BowDraw.isFull(player);
        if ((full && !BowMath.canOvercharge(BowDraw.overcharge(player))) || !HitGate.ready(player, HitKind.BOW)) {
            return;
        }
        if (full) {
            BowDraw.addOvercharge(player);
        } else {
            BowDraw.add(player, WeakSpotConfig.server.bow.bowHitTicks);
        }
        int combo = HitGate.accept(player, HitKind.BOW, player.blockPosition(), streak);
        // 乗っていれば、加速も続ける（騎射）
        VehicleHits.boostFromRider(player, combo);
    }

}

package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.ThrowCharge;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

/**
 * 投げる物の弱点のヒット通知の検証と効果（論理サーバー。1.7.0）。メインハンドに投げる物を持っている間に受け付け、
 * 次の 1 投の溜めを throwChargePerHit × コンボの掛け数だけ増やす（ThrowCharge。上限なし）。乗っていれば、乗り物の
 * 加速も続ける（騎射と同じ）。照準の角度は確かめない。
 */
public final class ThrowHits {



    private ThrowHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.THROW)
                || !ThrowCharge.CHARGES.isHolding(player) || player.isHandActive()) {
            return;
        }
        if (!HitGate.ready(player, HitKind.THROW)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.THROW, new BlockPos(player), streak);
        ThrowCharge.CHARGES.add(player, WeakSpotConfig.server.throwing.throwChargePerHit * ComboFactor.factor(combo),
                0);
        VehicleHits.boostFromRider(player, combo);
    }


    /** ログアウトの後片付け（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave != HitGate.Leave.RESPAWN) {
            ThrowCharge.CHARGES.clear(player);
        }
    }
}

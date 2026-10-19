package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.ThrowCharge;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

/** 照準の角度は確かめない。 */
public final class ThrowHits {

    private ThrowHits() {
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.THROW)
                || !ThrowCharge.CHARGES.isHolding(player) || player.isHandActive()
                || !HitGate.ready(player, HitKind.THROW)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.THROW, new BlockPos(player), streak);
        ThrowCharge.CHARGES.add(player, WeakSpotConfig.server.throwing.throwChargePerHit * ComboFactor.factor(combo),
                0);
        VehicleHits.boostFromRider(player, combo);
    }

    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave != HitGate.Leave.RESPAWN) {
            ThrowCharge.CHARGES.clear(player);
        }
    }
}

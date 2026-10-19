package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.ThrowCharge;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 照準の角度は確かめない。 */
public final class ThrowHits {

    private ThrowHits() {
    }

    public static void onHit(ServerPlayer player, int streak) {
        if (!HitGate.allowed(player, HitKind.THROW)
                || !ThrowCharge.CHARGES.isHolding(player) || player.isUsingItem()
                || !HitGate.ready(player, HitKind.THROW)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.THROW, player.blockPosition(), streak);
        ThrowCharge.CHARGES.add(player, WeakSpotConfig.server.throwing.throwChargePerHit * ComboFactor.factor(combo),
                0);
        VehicleHits.boostFromRider(player, combo);
    }

    static void forget(Player player, HitGate.Leave leave) {
        if (leave != HitGate.Leave.RESPAWN) {
            ThrowCharge.CHARGES.clear(player);
        }
    }
}

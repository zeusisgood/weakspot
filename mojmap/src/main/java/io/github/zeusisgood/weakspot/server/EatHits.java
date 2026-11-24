package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.EatDraw;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.server.level.ServerPlayer;

/** 照準の角度は確かめない。 */
public final class EatHits {

    private EatHits() {
    }

    public static void onHit(ServerPlayer player, int streak) {
        if (!HitGate.allowed(player, HitKind.EAT) || WeakSpotConfig.server.eat.eatHitTicks <= 0
                || !EatDraw.isEating(player) || !HitGate.ready(player, HitKind.EAT)) {
            return;
        }
        EatDraw.add(player, WeakSpotConfig.server.eat.eatHitTicks);
        int combo = HitGate.accept(player, HitKind.EAT, player.blockPosition(), streak);
        VehicleHits.boostFromRider(player, combo);
    }

}

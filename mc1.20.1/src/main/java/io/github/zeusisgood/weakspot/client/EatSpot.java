package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.EatDraw;
import io.github.zeusisgood.weakspot.VehicleTargets;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** クライアントもサーバーの返事を待たずに縮める。 */
final class EatSpot extends AimSpotKind {

    EatSpot() {
        super(HitKind.EAT, new HudSpot(HitKind.EAT));
    }

    @Override
    boolean blockedByUsingHand() {
        return false;
    }

    @Override
    boolean wanted(LocalPlayer player, SyncedSettings settings) {
        return settings.eatHitTicks > 0 && EatDraw.isEating(player);
    }

    @Override
    int placement(LocalPlayer player) {
        return VehicleTargets.isSteeredByLook(player) ? HudSpot.VERTICAL : HudSpot.FREE;
    }

    @Override
    void onHit(Minecraft mc, LocalPlayer player, SyncedSettings settings, int streak) {
        EatDraw.add(player, settings.eatHitTicks);
        VehicleSpot.onRiderHit(streak);
    }
}

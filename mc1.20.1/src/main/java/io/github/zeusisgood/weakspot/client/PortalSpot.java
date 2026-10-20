package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** 当てると、サーバーが移動までの待ち時間を縮める（PortalHits）。紫のゆらぎも、同じだけ進める。 */
final class PortalSpot extends AimSpotKind {

    /** バニラの紫のゆらぎが 1 tick に進む量。 */
    private static final float NAUSEA_PER_TICK = 0.0125F;

    PortalSpot() {
        super(HitKind.PORTAL, new HudSpot(HitKind.PORTAL));
    }

    /** 紫のゆらぎが進んでいるか、1.0 で止まっているか。 */
    private static boolean inPortal(LocalPlayer player) {
        return player.spinningEffectIntensity > 0
                && player.spinningEffectIntensity >= player.oSpinningEffectIntensity;
    }

    @Override
    boolean wanted(LocalPlayer player, SyncedSettings settings) {
        return !player.isPassenger() && inPortal(player);
    }

    @Override
    int placement(LocalPlayer player) {
        return HudSpot.FREE;
    }

    @Override
    void onHit(Minecraft mc, LocalPlayer player, SyncedSettings settings, int streak) {
        double ticks = settings.portalHitTicks * ComboFactor.factor(streak);
        player.spinningEffectIntensity = Math.min(1.0F,
                player.spinningEffectIntensity + (float) (ticks * NAUSEA_PER_TICK));
    }
}

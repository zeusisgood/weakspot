package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/** 当てると、サーバーが移動までの待ち時間を縮める（PortalHits）。紫のゆらぎも、同じだけ進める。 */
final class PortalSpot extends AimSpotKind {

    /** バニラの紫のゆらぎが 1 tick に進む量。 */
    private static final float NAUSEA_PER_TICK = 0.0125F;

    PortalSpot() {
        super(HitKind.PORTAL, new HudSpot(HitKind.PORTAL));
    }

    /** 紫のゆらぎが進んでいるか、1.0 で止まっているか。 */
    private static boolean inPortal(EntityPlayerSP player) {
        return player.timeInPortal > 0 && player.timeInPortal >= player.prevTimeInPortal;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return !player.isRiding() && inPortal(player);
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.FREE;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        double ticks = settings.portalHitTicks * ComboFactor.factor(streak);
        player.timeInPortal = Math.min(1.0F, player.timeInPortal + (float) (ticks * NAUSEA_PER_TICK));
    }
}

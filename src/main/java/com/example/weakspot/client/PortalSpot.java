package com.example.weakspot.client;

import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * ネザーゲートの弱点（自分だけ。1.8.0）。ゲートの中に立っている間（紫のゆらぎ timeInPortal が 0 より大きく、
 * 減っていない）、照準から 10〜20 度の所に金の弱点を出し、照準を合わせるだけでヒットにする。当てると、サーバーが
 * 移動までの待ち時間を縮める（PortalHits）。紫のゆらぎも、同じだけ進める（1 tick に 0.0125 進むので、tick × 0.0125）。
 * 1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class PortalSpot extends AimSpotKind {

    /** バニラの紫のゆらぎが 1 tick に進む量。 */
    private static final float NAUSEA_PER_TICK = 0.0125F;

    PortalSpot() {
        super(HitKind.PORTAL, new HudSpot(HitKind.PORTAL));
    }

    /** ゲートの中にいるか（紫のゆらぎが進んでいるか、1.0 で止まっているか）。 */
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

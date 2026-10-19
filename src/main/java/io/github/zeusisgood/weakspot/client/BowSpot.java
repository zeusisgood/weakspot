package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.BowDraw;
import io.github.zeusisgood.weakspot.VehicleTargets;
import io.github.zeusisgood.weakspot.common.BowMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
* 引き切ったあとは、過剰チャージが上限に届くまで弱点を出す。
* 引きゲージは、弓の弱点や一時オフに関係なく、bowDrawBarEnabled なら照準の下に出す。
*/
final class BowSpot extends AimSpotKind {

    /** 色を書き換えていなければ、この色のまま。 */
    private static final float[] DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] RING = {1.0F, 0.9F, 0.4F};
    private static final float[] CENTER = {1.0F, 0.95F, 0.7F};
    private static final int BAR_DRAWING = 0xFF8C42;
    private static final int BAR_FULL = 0xFFD23F;

    BowSpot() {
        super(HitKind.BOW, new HudSpot(HitKind.BOW, 0xFF5926).withPalette(DISK, RING, CENTER));
    }

    @Override
    boolean blockedByUsingHand() {
        return false;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return settings.bowHitTicks > 0 && BowDraw.isDrawing(player)
                && spotWanted(player);
    }

    private static boolean spotWanted(EntityPlayerSP player) {
        return !BowMath.isFull(BowDraw.usedTicks(player)) || BowMath.canOvercharge(BowDraw.overcharge(player));
    }

    /** 馬・豚に乗っているときは、照準の真上か真下だけ（狙うたびに進む向きがぶれないように）。 */
    @Override
    int placement(EntityPlayerSP player) {
        return VehicleTargets.isSteeredByLook(player) ? HudSpot.VERTICAL_FIXED : HudSpot.FREE;
    }

    @Override
    boolean canAim(Minecraft mc) {
        return !mc.gameSettings.hideGUI;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        // 乗っていれば、加速も続ける（騎射）
        VehicleSpot.onRiderHit(streak);
        // サーバーの返事を待たずに、自分の側でも引きを進める（弓の見た目とゲージのため）
        if (BowMath.isFull(BowDraw.usedTicks(player))) {
            BowDraw.addOvercharge(player);
        } else {
            BowDraw.add(player, settings.bowHitTicks);
        }
    }

    @Override
    boolean keepAfterHit(EntityPlayerSP player) {
        return spotWanted(player);
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.bowDrawBarEnabled && BowDraw.isDrawing(mc.player);
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        int used = mc.player.getItemInUseMaxCount();
        boolean full = BowMath.isFull(used);
        HudSpot.gauge(mc, full ? 1.0 : BowMath.barValue(used + partialTicks), full ? BAR_FULL : BAR_DRAWING, false);
        int overcharge = BowDraw.overcharge(mc.player);
        if (full && (overcharge > 0 || spot.has())) {
            ChargeGauge.drawMarks(mc, overcharge, BowMath.MAX_OVERCHARGE_HITS);
        }
    }
}

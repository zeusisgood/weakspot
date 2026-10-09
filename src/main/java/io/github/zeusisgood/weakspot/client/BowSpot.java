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
 * 弓の弱点（自分だけ。他のプレイヤーには見せない）と、引きゲージ。弓を引いている間、照準の近く（視線から 10〜20 度。
 * 1.5.4）に弱点を出し、照準を合わせるだけでヒットにする（クリックは要らない）。ヒットすると、弓の引きが進む（BowDraw）。
 * 引き切ったあとは、過剰チャージ（矢のダメージ +10%、上限 5 回。1.3.4）が上限に届くまで弱点を出す。
 * 馬・豚に乗っているときは照準の真上か真下だけで、yaw は出した時点の視線のまま（HudSpot.VERTICAL_FIXED）。
 * 引きゲージは、弓の弱点や一時オフに関係なく、bowDrawBarEnabled なら照準の下に出す。
 * 1.8.6 から AimSpotKind（AimSpots が回す。描き方・当たり判定は HudSpot。それまでは自前で持っていた）。
 */
final class BowSpot extends AimSpotKind {

    /** 弱点の色（円 #FF5926・輪・中心）。色を書き換えていなければ、この色のまま。 */
    private static final float[] DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] RING = {1.0F, 0.9F, 0.4F};
    private static final float[] CENTER = {1.0F, 0.95F, 0.7F};
    /** 引きゲージの、引いている途中 #FF8C42 と、引き切った #FFD23F。 */
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

    /** 弱点を出すか。引き切る前と、引き切ったあとの過剰チャージが上限に届くまで。 */
    private static boolean spotWanted(EntityPlayerSP player) {
        return !BowMath.isFull(BowDraw.usedTicks(player)) || BowMath.canOvercharge(BowDraw.overcharge(player));
    }

    /** 馬・豚に乗っているときは、照準の真上か真下だけ（1.6.0。狙うたびに進む向きがぶれないように）。 */
    @Override
    int placement(EntityPlayerSP player) {
        return VehicleTargets.isSteeredByLook(player) ? HudSpot.VERTICAL_FIXED : HudSpot.FREE;
    }


    /** F1 で画面を隠している間は当てない（今までどおり）。 */
    @Override
    boolean canAim(Minecraft mc) {
        return !mc.gameSettings.hideGUI;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        // 乗り物に乗っていれば、加速も続ける（騎射。1.6.0）
        VehicleSpot.onRiderHit(streak);
        // サーバーの返事を待たずに、自分の側でも引きを進める（弓の見た目とゲージのため）。引き切ったあとは過剰チャージ
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

    /** 照準の下の、引き具合のゲージ。左から伸び、引き切ったら色が変わる。引き切ったあとは、過剰チャージの目盛りも出す。 */
    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        int used = mc.player.getItemInUseMaxCount();
        boolean full = BowMath.isFull(used);
        HudSpot.gauge(mc, full ? 1.0 : BowMath.barValue(used + partialTicks), full ? BAR_FULL : BAR_DRAWING, false);
        int overcharge = BowDraw.overcharge(mc.player);
        if (full && (overcharge > 0 || spot.has())) {
            // 過剰チャージの目盛り。達した分を赤く
            ChargeGauge.drawMarks(mc, overcharge, BowMath.MAX_OVERCHARGE_HITS);
        }
    }
}

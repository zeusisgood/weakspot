package com.example.weakspot.client;

import com.example.weakspot.BowDraw;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.common.BowMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;

/**
 * 弓の弱点（自分だけ。他のプレイヤーには見せない）と、引きゲージ。弓を引いている間、照準の近く（視線から 10〜20 度。
 * 1.5.4）に弱点を出し、照準を合わせるだけでヒットにする（クリックは要らない）。ヒットすると、弓の引きが進む（BowDraw）。
 * 引き切ったあとは、過剰チャージ（矢のダメージ +10%、上限 5 回。1.3.4）が上限に届くまで弱点を出す。
 * 馬・豚に乗っているときは照準の真上か真下だけで、yaw は出した時点の視線のまま（HudSpot.VERTICAL_FIXED）。
 * 引きゲージは、弓の弱点や一時オフに関係なく、bowDrawBarEnabled なら照準の下に出す。
 * 1.8.6 から AimSpotKind（AimSpots が回す。描き方・当たり判定は HudSpot。それまでは自前で持っていた）。
 */
final class BowSpot extends AimSpotKind {

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 3;
    /** 照準の中心から、ゲージの中心までの下向きの距離（GUI ピクセル）。 */
    private static final int BAR_OFFSET = 12;

    /** 弱点の色（円 #FF5926・輪・中心）。色を書き換えていなければ、この色のまま。 */
    private static final float[] DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] RING = {1.0F, 0.9F, 0.4F};
    private static final float[] CENTER = {1.0F, 0.95F, 0.7F};
    /** 引いている途中 #FF8C42、引き切った #FFD23F、背景 #1E1E1E 半透明。 */
    private static final float[] BAR_DRAWING = {0xFF / 255F, 0x8C / 255F, 0x42 / 255F, 1.0F};
    private static final float[] BAR_FULL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] BAR_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};
    /** 過剰チャージの目盛り #FF4D4D。ゲージのすぐ下に、上限の数だけ並べる。 */
    private static final float[] OVERCHARGE = {0xFF / 255F, 0x4D / 255F, 0x4D / 255F, 1.0F};
    private static final int OVERCHARGE_HEIGHT = 2;
    private static final int OVERCHARGE_GAP = 1;

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
        return WeakSpotConfig.bowDrawBarEnabled && BowDraw.isDrawing(mc.player);
    }

    /** 照準の下の、引き具合のゲージ。左から伸び、引き切ったら色が変わる。引き切ったあとは、過剰チャージの目盛りも出す。 */
    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        ScaledResolution res = new ScaledResolution(mc);
        int used = mc.player.getItemInUseMaxCount();
        boolean full = BowMath.isFull(used);
        double value = full ? 1.0 : BowMath.barValue(used + partialTicks);
        double x0 = res.getScaledWidth() / 2.0 - BAR_WIDTH / 2.0;
        double y0 = res.getScaledHeight() / 2.0 + BAR_OFFSET - BAR_HEIGHT / 2.0;
        ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH, y0 + BAR_HEIGHT, BAR_BACK);
        if (value > 0) {
            ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH * value, y0 + BAR_HEIGHT, full ? BAR_FULL : BAR_DRAWING);
        }
        int overcharge = BowDraw.overcharge(mc.player);
        if (full && (overcharge > 0 || spot.has())) {
            // 過剰チャージの目盛り。達した分を赤く
            int count = BowMath.MAX_OVERCHARGE_HITS;
            double width = (BAR_WIDTH - OVERCHARGE_GAP * (count - 1)) / (double) count;
            double top = y0 + BAR_HEIGHT + OVERCHARGE_GAP;
            for (int i = 0; i < count; i++) {
                double left = x0 + i * (width + OVERCHARGE_GAP);
                ScreenProjection.rect(left, top, left + width, top + OVERCHARGE_HEIGHT,
                        i < overcharge ? OVERCHARGE : BAR_BACK);
            }
        }
    }
}

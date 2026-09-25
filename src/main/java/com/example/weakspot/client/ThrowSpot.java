package com.example.weakspot.client;

import com.example.weakspot.ThrowCharge;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * 投げる物の弱点（自分だけ。1.7.0）。エンダーパール・雪玉などをメインハンドに持っている間、照準から 10〜20 度
 * （馬・豚に乗っているときは真上か真下だけ）に青緑の弱点を出し、照準を合わせるだけでヒットにする。当てるたびに
 * 次の 1 投の溜めが増え（ThrowCharge。クライアントもサーバーの返事を待たずに進める）、照準の下にゲージを出す。
 * 1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class ThrowSpot extends AimSpotKind {

    /** 青緑 #2ED3B7。 */
    private static final int RGB = 0x2ED3B7;
    /** ゲージ 1 本分の溜め（倍率 ×3）。越えた分は、1 本分ごとに赤い目盛りを 1 つ足す。 */
    private static final double CHARGE_PER_BAR = 2.0;

    /** このフレームのゲージの、本数を越えた分の文字（drawGauge から drawAfterOverlay へ渡す）。 */
    private String extra;

    ThrowSpot() {
        super(HitKind.THROW, new HudSpot(HitKind.THROW, RGB));
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return settings.throwWeakSpotEnabled && ThrowCharge.isHoldingThrowable(player);
    }

    @Override
    int placement(EntityPlayerSP player) {
        return VehicleTargets.isSteeredByLook(player) ? HudSpot.VERTICAL : HudSpot.FREE;
    }

    @Override
    int minHitInterval(SyncedSettings settings) {
        return settings.throwMinHitIntervalTicks;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        ThrowCharge.add(player, settings.throwChargePerHit * ComboFactor.factor(streak));
        VehicleSpot.onRiderHit(streak);
    }

    private static double charge(Minecraft mc) {
        return ThrowCharge.isHoldingThrowable(mc.player) ? ThrowCharge.amount(mc.player) : 0;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.throwChargeBarEnabled && charge(mc) > 0;
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        extra = ChargeGauge.drawBars(mc, charge(mc), CHARGE_PER_BAR, MarkerLook.color(HitKind.THROW, RGB));
    }

    @Override
    void drawAfterOverlay(Minecraft mc, float partialTicks) {
        ChargeGauge.drawLabels(mc, charge(mc), MarkerLook.color(HitKind.THROW, RGB), extra);
    }
}

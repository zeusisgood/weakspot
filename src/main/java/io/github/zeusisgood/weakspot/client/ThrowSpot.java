package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.ThrowCharge;
import io.github.zeusisgood.weakspot.VehicleTargets;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/** クライアントもサーバーの返事を待たずに溜める。 */
final class ThrowSpot extends AimSpotKind {

    /** 倍率 ×3。 */
    private static final double CHARGE_PER_BAR = 2.0;

    ThrowSpot() {
        super(HitKind.THROW, new HudSpot(HitKind.THROW));
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return ThrowCharge.CHARGES.isHolding(player);
    }

    @Override
    int placement(EntityPlayerSP player) {
        return VehicleTargets.isSteeredByLook(player) ? HudSpot.VERTICAL : HudSpot.FREE;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        ThrowCharge.CHARGES.add(player, settings.throwChargePerHit * ComboFactor.factor(streak), 0);
        VehicleSpot.onRiderHit(streak);
    }

    private static double charge(Minecraft mc) {
        return ThrowCharge.CHARGES.isHolding(mc.player) ? ThrowCharge.CHARGES.amount(mc.player) : 0;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.throwChargeBarEnabled && charge(mc) > 0;
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        ChargeGauge.drawBars(mc, charge(mc), CHARGE_PER_BAR, MarkerLook.color(HitKind.THROW));
    }

    @Override
    void drawAfterOverlay(Minecraft mc, float partialTicks) {
        ChargeGauge.drawLabels(mc, charge(mc), MarkerLook.color(HitKind.THROW));
    }
}

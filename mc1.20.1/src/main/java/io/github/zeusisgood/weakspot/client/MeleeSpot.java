package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.MeleeCharge;
import io.github.zeusisgood.weakspot.MeleeTargets;
import io.github.zeusisgood.weakspot.VehicleTargets;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * 照準の左右だけに出す（走りの弱点の上下と見分けるため）。馬・豚に乗っているときは出さない（乗り物の弱点が上下に出ているため）。
 * クライアントもサーバーの返事を待たずに溜める。
 */
final class MeleeSpot extends AimSpotKind {

    /** ブロック。 */
    private static final double ENEMY_RANGE = 16;
    /** 倍率 ×2。 */
    private static final double CHARGE_PER_BAR = 1.0;

    /** tick ごとに探し直す。 */
    private boolean enemyNear;

    MeleeSpot() {
        super(HitKind.MELEE, new HudSpot(HitKind.MELEE));
    }

    @Override
    void clear() {
        super.clear();
        enemyNear = false;
    }

    @Override
    void beforeTick(Minecraft mc) {
        enemyNear = MeleeCharge.CHARGES.isHolding(mc.player) && MeleeTargets.hasEnemyNear(mc.player, ENEMY_RANGE);
    }

    @Override
    boolean wanted(LocalPlayer player, SyncedSettings settings) {
        return MeleeCharge.CHARGES.isHolding(player) && enemyNear
                && !VehicleTargets.isSteeredByLook(player);
    }

    @Override
    int placement(LocalPlayer player) {
        return HudSpot.HORIZONTAL;
    }

    @Override
    void onHit(Minecraft mc, LocalPlayer player, SyncedSettings settings, int streak) {
        MeleeCharge.CHARGES.add(player, settings.meleeChargePerHit * ComboFactor.factor(streak),
                settings.meleeChargeMax);
    }

    private static double charge(Minecraft mc) {
        return MeleeCharge.CHARGES.isHolding(mc.player) ? MeleeCharge.CHARGES.amount(mc.player) : 0;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.meleeChargeBarEnabled && charge(mc) > 0;
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        ChargeGauge.drawBars(mc, charge(mc), CHARGE_PER_BAR, MarkerLook.color(HitKind.MELEE));
    }

    @Override
    void drawAfterOverlay(GuiGraphics graphics, Minecraft mc, float partialTicks) {
        ChargeGauge.drawLabels(graphics, mc, charge(mc), MarkerLook.color(HitKind.MELEE));
    }
}

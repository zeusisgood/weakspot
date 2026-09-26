package com.example.weakspot.client;

import com.example.weakspot.MeleeCharge;
import com.example.weakspot.MeleeTargets;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * 近接の弱点（自分だけ。1.8.0 で、敵の体の弱点から置き換えた）。剣か斧を持ち、16 ブロック以内に敵がいる間、照準の
 * 左右だけに銀の弱点を出し（走りの弱点の上下と見分けるため）、照準を合わせるだけでヒットにする。当てるたびに
 * 次の攻撃の溜めが増え（MeleeCharge。クライアントもサーバーの返事を待たずに溜める）、照準の下にゲージを出す。
 * 馬・豚に乗っているときは出さない（乗り物の弱点が上下に出ているため）。1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class MeleeSpot extends AimSpotKind {

    /** 銀 #D0D8E0。 */
    private static final int RGB = 0xD0D8E0;
    /** 敵を探す距離（ブロック）。 */
    private static final double ENEMY_RANGE = 16;
    /** ゲージ 1 本分の溜め（倍率 ×2）。 */
    private static final double CHARGE_PER_BAR = 1.0;

    /** 近くに敵がいるか（tick ごとに探し直す）。 */
    private boolean enemyNear;
    private String extra;

    MeleeSpot() {
        super(HitKind.MELEE, new HudSpot(HitKind.MELEE, RGB));
    }

    @Override
    void clear() {
        super.clear();
        enemyNear = false;
    }

    @Override
    void beforeTick(Minecraft mc) {
        enemyNear = MeleeCharge.isHoldingWeapon(mc.player) && MeleeTargets.hasEnemyNear(mc.player, ENEMY_RANGE);
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return MeleeCharge.isHoldingWeapon(player) && enemyNear
                && !VehicleTargets.isSteeredByLook(player);
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.HORIZONTAL;
    }


    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        MeleeCharge.add(player, settings.meleeChargePerHit * ComboFactor.factor(streak), settings.meleeChargeMax);
    }

    private static double charge(Minecraft mc) {
        return MeleeCharge.isHoldingWeapon(mc.player) ? MeleeCharge.amount(mc.player) : 0;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.meleeChargeBarEnabled && charge(mc) > 0;
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        extra = ChargeGauge.drawBars(mc, charge(mc), CHARGE_PER_BAR, MarkerLook.color(HitKind.MELEE, RGB));
    }

    @Override
    void drawAfterOverlay(Minecraft mc, float partialTicks) {
        ChargeGauge.drawLabels(mc, charge(mc), MarkerLook.color(HitKind.MELEE, RGB), extra);
    }
}

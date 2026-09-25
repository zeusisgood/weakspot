package com.example.weakspot.client;

import com.example.weakspot.PlayerRules;
import com.example.weakspot.MeleeCharge;
import com.example.weakspot.MeleeTargets;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 近接の弱点（自分だけ。1.8.0 で、敵の体の弱点から置き換えた）。剣か斧を持ち、16 ブロック以内に敵がいる間、照準の
 * 左右だけに銀の弱点を出し（走りの弱点の上下と見分けるため）、照準を合わせるだけでヒットにする。当てるたびに
 * 次の攻撃の溜めが増え（MeleeCharge。クライアントもサーバーの返事を待たずに溜める）、照準の下にゲージを出す。
 * 馬・豚に乗っているときは出さない（乗り物の弱点が上下に出ているため）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class MeleeSpot {

    /** 銀 #D0D8E0。 */
    private static final int RGB = 0xD0D8E0;
    /** 敵を探す距離（ブロック）。 */
    private static final double ENEMY_RANGE = 16;
    /** ゲージ 1 本分の溜め（倍率 ×2）。 */
    private static final double CHARGE_PER_BAR = 1.0;

    private static final HudSpot SPOT = new HudSpot(HitKind.MELEE, RGB);
    /** 近くに敵がいるか（tick ごとに探し直す）。 */
    private static boolean enemyNear;

    private MeleeSpot() {
    }

    static void clear() {
        SPOT.clear();
        enemyNear = false;
    }

    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.MELEE)
                || !PlayerRules.canUse(player) || player.isHandActive()) {
            return false;
        }
        return ClientSettings.get().meleeWeakSpotEnabled && MeleeCharge.isHoldingWeapon(player) && enemyNear
                && !VehicleTargets.isSteeredByLook(player);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || mc.isGamePaused() || mc.player == null) {
            return;
        }
        enemyNear = MeleeCharge.isHoldingWeapon(mc.player) && MeleeTargets.hasEnemyNear(mc.player, ENEMY_RANGE);
        if (!eligible(mc)) {
            SPOT.clear();
        } else {
            SPOT.ensureHorizontal(mc.player);
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.MELEE, settings.meleeMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.MELEE);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.MELEE, streak));
        MeleeCharge.add(mc.player, settings.meleeChargePerHit * ComboFactor.factor(streak),
                settings.meleeChargeMax);
        SPOT.relocate(mc.player);
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI) {
            return;
        }
        double charge = MeleeCharge.isHoldingWeapon(mc.player) ? MeleeCharge.amount(mc.player) : 0;
        boolean bar = WeakSpotConfig.meleeChargeBarEnabled && charge > 0;
        if (!SPOT.has() && !bar) {
            return;
        }
        HudSpot.beginOverlay();
        SPOT.draw(mc);
        int rgb = MarkerLook.color(HitKind.MELEE, RGB);
        String extra = bar ? ChargeGauge.drawBars(mc, charge, CHARGE_PER_BAR, rgb) : null;
        HudSpot.endOverlay();
        if (bar) {
            ChargeGauge.drawLabels(mc, charge, rgb, extra);
        }
    }
}

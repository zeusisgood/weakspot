package com.example.weakspot.client;

import com.example.weakspot.ThrowCharge;
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
 * 投げる物の弱点（自分だけ。1.7.0）。エンダーパール・雪玉などをメインハンドに持っている間、照準から 10〜20 度
 * （馬・豚に乗っているときは真上か真下だけ）に青緑の弱点を出し、照準を合わせるだけでヒットにする。当てるたびに
 * 次の 1 投の溜めが増え（ThrowCharge。クライアントもサーバーの返事を待たずに進める）、照準の下にゲージを出す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class ThrowSpot {

    /** ティール #2ED3B7。 */
    private static final int RGB = 0x2ED3B7;
    /** ゲージ 1 本分の溜め（倍率 ×3）。越えた分は、1 本分ごとに赤い目盛りを 1 つ足す。 */
    private static final double CHARGE_PER_BAR = 2.0;

    private static final HudSpot SPOT = new HudSpot(HitKind.THROW, RGB);

    private ThrowSpot() {
    }

    static void clear() {
        SPOT.clear();
    }

    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.THROW)
                || player.capabilities.isCreativeMode || player.isSpectator() || player.isHandActive()) {
            return false;
        }
        return ClientSettings.get().throwWeakSpotEnabled && ThrowCharge.isHoldingThrowable(player);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || mc.isGamePaused() || mc.player == null) {
            return;
        }
        if (!eligible(mc)) {
            SPOT.clear();
        } else {
            SPOT.ensure(mc.player, VehicleTargets.isSteeredByLook(mc.player));
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.THROW, settings.throwMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.THROW);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.THROW, streak));
        ThrowCharge.add(mc.player, settings.throwChargePerHit * ComboFactor.factor(streak));
        VehicleSpot.onRiderHit(streak);
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
        double charge = ThrowCharge.isHoldingThrowable(mc.player) ? ThrowCharge.amount(mc.player) : 0;
        boolean bar = WeakSpotConfig.throwChargeBarEnabled && charge > 0;
        if (!SPOT.has() && !bar) {
            return;
        }
        HudSpot.beginOverlay();
        SPOT.draw(mc);
        int rgb = MarkerLook.color(HitKind.THROW, RGB);
        String extra = bar ? ChargeGauge.drawBars(mc, charge, CHARGE_PER_BAR, rgb) : null;
        HudSpot.endOverlay();
        if (bar) {
            ChargeGauge.drawLabels(mc, charge, rgb, extra);
        }
    }
}

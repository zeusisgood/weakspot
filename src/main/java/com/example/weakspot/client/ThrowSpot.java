package com.example.weakspot.client;

import com.example.weakspot.ThrowCharge;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MachineComboBoost;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
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
    /** 目盛りを並べる数。越えたら「×n」の数字で出す。 */
    private static final int MAX_MARKS = 5;
    private static final int MARK_HEIGHT = 2;
    private static final int MARK_GAP = 1;
    /** 目盛りの赤 #FF4D4D（弓の過剰チャージと同じ）と、背景 #1E1E1E 半透明。 */
    private static final float[] MARK = {0xFF / 255F, 0x4D / 255F, 0x4D / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

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
        ThrowCharge.add(mc.player, settings.throwChargePerHit * MachineComboBoost.factor(streak));
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
        String extra = null;
        if (bar) {
            HudSpot.gauge(mc, charge / CHARGE_PER_BAR, rgb, false);
            int marks = (int) Math.floor(charge / CHARGE_PER_BAR) - 1;
            if (marks > MAX_MARKS) {
                extra = "×" + marks;
            } else if (marks > 0) {
                drawMarks(mc, marks);
            }
        }
        HudSpot.endOverlay();
        if (bar) {
            drawLabels(mc, charge, rgb, extra);
        }
    }

    /** ゲージのすぐ下に、1 本を越えた分の目盛りを並べる（弓の過剰チャージと同じ形）。 */
    private static void drawMarks(Minecraft mc, int marks) {
        ScaledResolution res = new ScaledResolution(mc);
        double x0 = res.getScaledWidth() / 2.0 - HudSpot.GAUGE_WIDTH / 2.0;
        double top = res.getScaledHeight() / 2.0 + HudSpot.GAUGE_OFFSET + HudSpot.GAUGE_HEIGHT / 2.0 + MARK_GAP;
        double width = (HudSpot.GAUGE_WIDTH - MARK_GAP * (MAX_MARKS - 1)) / (double) MAX_MARKS;
        for (int i = 0; i < MAX_MARKS; i++) {
            double left = x0 + i * (width + MARK_GAP);
            ScreenProjection.rect(left, top, left + width, top + MARK_HEIGHT, i < marks ? MARK : BACK);
        }
    }

    /** ゲージの右に今の倍率「×3.0」、目盛りが多すぎるときは、ゲージの下に「×n」の数を出す。 */
    private static void drawLabels(Minecraft mc, double charge, int rgb, String extra) {
        ScaledResolution res = new ScaledResolution(mc);
        int right = res.getScaledWidth() / 2 + HudSpot.GAUGE_WIDTH / 2 + 3;
        int y = res.getScaledHeight() / 2 + HudSpot.GAUGE_OFFSET - 4;
        String label = String.format(Locale.ROOT, "×%.1f", 1 + charge);
        mc.fontRenderer.drawStringWithShadow(label, right, y, rgb);
        if (extra != null) {
            int width = mc.fontRenderer.getStringWidth(extra);
            mc.fontRenderer.drawStringWithShadow(extra, res.getScaledWidth() / 2 - width / 2, y + 9, 0xFF4D4D);
        }
    }
}

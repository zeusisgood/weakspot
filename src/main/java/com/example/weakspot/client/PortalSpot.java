package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.config.SyncedSettings;
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
 * ネザーゲートの弱点（自分だけ。1.8.0）。ゲートの中に立っている間（紫のゆらぎ timeInPortal が 0 より大きく、
 * 減っていない）、照準から 10〜20 度の所に金の弱点を出し、照準を合わせるだけでヒットにする。当てると、サーバーが
 * 移動までの待ち時間を縮める（PortalHits）。紫のゆらぎも、同じだけ進める（1 tick に 0.0125 進むので、tick × 0.0125）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class PortalSpot {

    /** 金 #FFD23F（ゲートの紫の上で目立つ）。 */
    private static final int RGB = 0xFFD23F;
    /** バニラの紫のゆらぎが 1 tick に進む量。 */
    private static final float NAUSEA_PER_TICK = 0.0125F;

    private static final HudSpot SPOT = new HudSpot(HitKind.PORTAL, RGB);

    private PortalSpot() {
    }

    static void clear() {
        SPOT.clear();
    }

    /** ゲートの中にいるか（紫のゆらぎが進んでいるか、1.0 で止まっているか）。 */
    private static boolean inPortal(EntityPlayerSP player) {
        return player.timeInPortal > 0 && player.timeInPortal >= player.prevTimeInPortal;
    }

    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.PORTAL)
                || player.capabilities.isCreativeMode || player.isSpectator() || player.isHandActive()
                || player.isRiding()) {
            return false;
        }
        return ClientSettings.get().portalWeakSpotEnabled && inPortal(player);
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
            SPOT.ensure(mc.player, false);
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.PORTAL, settings.portalMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.PORTAL);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.PORTAL, streak));
        double ticks = settings.portalHitTicks * ComboFactor.factor(streak);
        EntityPlayerSP player = mc.player;
        player.timeInPortal = Math.min(1.0F, player.timeInPortal + (float) (ticks * NAUSEA_PER_TICK));
        SPOT.relocate(player);
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI || !SPOT.has()) {
            return;
        }
        HudSpot.beginOverlay();
        SPOT.draw(mc);
        HudSpot.endOverlay();
    }
}

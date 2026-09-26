package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

/**
 * 画面の上のマーカー（ScreenSpotKind）を、まとめて回す（1.8.8。それまでは睡眠・エンチャントが、それぞれにイベントを
 * 受けていた）。DrawScreenEvent.Post で描き、MouseInputEvent.Pre で左クリックが円の中ならキャンセルして当てる
 * （当てたクリックは、ボタンやスロットに届かない）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class ScreenSpots {

    /** 種類を足すときは、ここに足す。 */
    private static final ScreenSpotKind[] KINDS = {
            new SleepSpot(),
            new EnchantSpot(),
    };

    private ScreenSpots() {
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen gui = event.getGui();
        for (ScreenSpotKind k : KINDS) {
            if (!k.eligible(mc, gui)) {
                k.shownOn = null;
                continue;
            }
            ensure(k, gui);
            if (k.markerVisible(gui)) {
                draw(k);
            }
            k.drawExtra(mc, gui);
        }
    }

    /** 画面が変わったら（開き直したら）、新しい位置に出す。 */
    private static void ensure(ScreenSpotKind k, GuiScreen gui) {
        if (k.shownOn == gui) {
            return;
        }
        k.shownOn = gui;
        k.onShown();
        k.has = k.place(gui, gui.width / 2.0, gui.height / 2.0);
        k.motion.jumpTo(k.x, k.y);
    }

    private static void draw(ScreenSpotKind k) {
        long nowMs = Minecraft.getSystemTime();
        boolean trail = WeakSpotConfig.weakSpotTrailEnabled;
        float[][] look = k.look();
        MarkerShape shape = MarkerLook.shape(k.kind);
        double radius = ScreenSpotKind.RADIUS;
        HudSpot.beginOverlay();
        if (trail) {
            for (MarkerMotion.Afterimage image : k.motion.afterimages(nowMs)) {
                float a = (float) image.alpha(nowMs);
                ScreenProjection.fillShape(image.u, image.v, radius, shape, look[0], 0.35F * a);
                ScreenProjection.outlineShape(image.u, image.v, radius, shape, look[1], 0.5F * a);
            }
        }
        double[] p = trail ? k.motion.position(nowMs) : new double[] {k.x, k.y};
        float alpha = k.outlineAlpha();
        ScreenProjection.fillShape(p[0], p[1], radius, shape, look[0], 0.55F);
        ScreenProjection.outlineShape(p[0], p[1], radius, shape, look[1], alpha);
        if (shape.hasCenterDot()) {
            ScreenProjection.fill(p[0], p[1], radius * 0.3, look[2], alpha);
        }
        HudSpot.endOverlay();
    }

    @SubscribeEvent
    public static void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen gui = event.getGui();
        for (ScreenSpotKind k : KINDS) {
            if (!k.eligible(mc, gui) || k.shownOn != gui || !k.markerVisible(gui)) {
                continue;
            }
            double mouseX = Mouse.getEventX() * gui.width / (double) mc.displayWidth;
            double mouseY = gui.height - Mouse.getEventY() * gui.height / (double) mc.displayHeight - 1;
            if (Math.hypot(mouseX - k.x, mouseY - k.y) > ScreenSpotKind.RADIUS) {
                continue;
            }
            // 当てたクリックは、ボタンやスロットに届かないようにする
            event.setCanceled(true);
            if (!ClientWeakSpotHandler.canHitNow(k.kind, k.minHitInterval(ClientSettings.get()))) {
                return;
            }
            int streak = ClientWeakSpotHandler.registerHit(k.kind);
            WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(k.kind, streak));
            k.onHit();
            if (k.place(gui, k.x, k.y)) {
                if (WeakSpotConfig.weakSpotTrailEnabled) {
                    k.motion.moveTo(k.x, k.y, Minecraft.getSystemTime());
                } else {
                    k.motion.jumpTo(k.x, k.y);
                }
            }
            return;
        }
    }
}

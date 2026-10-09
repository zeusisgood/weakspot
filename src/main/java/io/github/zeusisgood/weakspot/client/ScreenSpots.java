package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * 画面の上のマーカー（ScreenSpotKind）を、まとめて回す（1.8.8。それまでは睡眠・エンチャントが、それぞれにイベントを
 * 受けていた）。DrawScreenEvent.Post で描く。重ねたら当たりの種類（睡眠。1.10.1）は、そこでカーソルが円の中なら当てる。
 * クリックで当てる種類（エンチャント）は、MouseInputEvent.Pre で左クリックが円の中ならキャンセルして当てる
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
        // コンテナ画面（エンチャント台など）は、アイテム用のライティングを有効にしたまま Post を出すので、
        // 切ってから描く（1.11.4。弱点の円・文字が暗くなっていた）。ほかの Mod のために、描き終えたら戻す
        boolean lit = GL11.glIsEnabled(GL11.GL_LIGHTING);
        if (lit) {
            GlStateManager.disableLighting();
        }
        for (ScreenSpotKind k : KINDS) {
            if (!k.eligible(mc, gui)) {
                k.shownOn = null;
                continue;
            }
            ensure(k, gui);
            if (k.markerVisible(gui)) {
                if (k.hitsOnHover() && inside(k, event.getMouseX(), event.getMouseY())) {
                    hit(k, gui);
                }
                draw(k);
            }
            k.drawExtra(mc, gui);
        }
        if (lit) {
            GlStateManager.enableLighting();
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
        boolean trail = WeakSpotConfig.client.markers.weakSpotTrailEnabled;
        float[][] look = k.look();
        MarkerShape shape = MarkerLook.shape(k.kind);
        double radius = k.radius();
        HudSpot.beginOverlay();
        if (trail) {
            for (MarkerMotion.Afterimage image : k.motion.afterimages(nowMs)) {
                ScreenProjection.drawAfterimage(image.u, image.v, radius, shape, look, (float) image.alpha(nowMs));
            }
        }
        double[] p = trail ? k.motion.position(nowMs) : new double[] {k.x, k.y};
        ScreenProjection.drawMarker(p[0], p[1], radius, shape, look, 0.55F, k.outlineAlpha());
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
            if (k.hitsOnHover() || !k.eligible(mc, gui) || k.shownOn != gui || !k.markerVisible(gui)) {
                continue;
            }
            double mouseX = Mouse.getEventX() * gui.width / (double) mc.displayWidth;
            double mouseY = gui.height - Mouse.getEventY() * gui.height / (double) mc.displayHeight - 1;
            if (!inside(k, mouseX, mouseY)) {
                continue;
            }
            // 当てたクリックは、ボタンやスロットに届かないようにする
            event.setCanceled(true);
            hit(k, gui);
            return;
        }
    }

    private static boolean inside(ScreenSpotKind k, double mouseX, double mouseY) {
        return Math.hypot(mouseX - k.x, mouseY - k.y) <= k.radius();
    }

    /** 当てる（最小間隔の中なら何もしない）。当てたら次の位置へ移す。 */
    private static void hit(ScreenSpotKind k, GuiScreen gui) {
        if (!OwnHits.canHit(k.kind, k.minHitInterval(ClientSettings.get()))) {
            return;
        }
        int streak = OwnHits.register(k.kind);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(k.kind, streak));
        k.onHit();
        if (k.place(gui, k.x, k.y)) {
            if (WeakSpotConfig.client.markers.weakSpotTrailEnabled) {
                k.motion.moveTo(k.x, k.y, Minecraft.getSystemTime());
            } else {
                k.motion.jumpTo(k.x, k.y);
            }
        }
    }
}

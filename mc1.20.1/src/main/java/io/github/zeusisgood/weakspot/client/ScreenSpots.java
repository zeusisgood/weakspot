package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.HitMessage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 重ねたら当たりの種類は ScreenEvent.Render.Post で、クリックで当てる種類は MouseButtonPressed.Pre で当てる。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class ScreenSpots {

    /** 種類を足すときは、ここに足す。 */
    private static final ScreenSpotKind[] KINDS = {
            new SleepSpot(),
            new EnchantSpot(),
    };

    private ScreenSpots() {
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Screen gui = event.getScreen();
        GuiGraphics graphics = event.getGuiGraphics();
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
                draw(k, graphics);
            }
            k.drawExtra(graphics, mc, gui);
        }
    }

    /** 開き直したら、新しい位置に出す。 */
    private static void ensure(ScreenSpotKind k, Screen gui) {
        if (k.shownOn == gui) {
            return;
        }
        k.shownOn = gui;
        k.has = k.place(gui, gui.width / 2.0, gui.height / 2.0);
        k.motion.jumpTo(k.x, k.y);
    }

    private static void draw(ScreenSpotKind k, GuiGraphics graphics) {
        long nowMs = Util.getMillis();
        boolean trail = WeakSpotConfig.client.markers.weakSpotTrailEnabled;
        float[][] look = k.look();
        MarkerShape shape = MarkerLook.shape(k.kind);
        double radius = k.radius();
        // 画面の溜まった描画（スロットのアイテム・文字）を先に出して、マーカーを上に重ねる
        graphics.flush();
        Draw.setPose(graphics.pose().last().pose());
        HudSpot.beginOverlay();
        if (trail) {
            for (MarkerMotion.Afterimage image : k.motion.afterimages(nowMs)) {
                ScreenProjection.drawAfterimage(image.u, image.v, radius, shape, look, (float) image.alpha(nowMs));
            }
        }
        double[] p = trail ? k.motion.position(nowMs) : new double[] {k.x, k.y};
        ScreenProjection.drawMarker(p[0], p[1], radius, shape, look, 0.55F, k.outlineAlpha());
        HudSpot.endOverlay();
        Draw.setPose(null);
    }

    @SubscribeEvent
    public static void onMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screen gui = event.getScreen();
        for (ScreenSpotKind k : KINDS) {
            if (k.hitsOnHover() || !k.eligible(mc, gui) || k.shownOn != gui || !k.markerVisible(gui)) {
                continue;
            }
            if (!inside(k, event.getMouseX(), event.getMouseY())) {
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

    /** 最小間隔の中なら何もしない。 */
    private static void hit(ScreenSpotKind k, Screen gui) {
        if (!OwnHits.canHit(k.kind, k.minHitInterval(ClientSettings.get()))) {
            return;
        }
        int streak = OwnHits.register(k.kind);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(k.kind, streak));
        if (k.place(gui, k.x, k.y)) {
            if (WeakSpotConfig.client.markers.weakSpotTrailEnabled) {
                k.motion.moveTo(k.x, k.y, Util.getMillis());
            } else {
                k.motion.jumpTo(k.x, k.y);
            }
        }
    }
}

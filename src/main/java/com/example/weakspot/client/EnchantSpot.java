package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

/**
 * エンチャントの弱点（1.7.0）。エンチャント台の画面で、候補が出ている間、画面の枠の外に紫のマーカーを出し、クリックで
 * 当てる（寝ている間の弱点と同じ。当てたクリックは、スロットや候補に届かないようにキャンセルする）。当てると、サーバーが
 * 3 つの候補を引き直す（EnchantHits。何も減らない）。枠の上に、引き直せることと回数の注釈を出す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class EnchantSpot {

    /** 紫 #B070FF。 */
    private static final int RGB = 0xB070FF;
    private static final double RADIUS = 12;
    /** エンチャント台の画面の枠の大きさ（GuiEnchantment の xSize / ySize）。 */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    /** 枠から離す距離と、画面の端から内側に入れる距離（GUI ピクセル）。 */
    private static final int PANEL_GAP = 8;
    private static final int EDGE = 16;

    private static final Random RANDOM = new Random();
    private static final MarkerMotion MOTION = new MarkerMotion(0, 0);

    private static GuiScreen shownOn;
    /** 今の位置。置ける場所がなければ has が false。 */
    private static boolean has;
    private static double x;
    private static double y;
    /** この画面を開いてから引き直した回数（注釈に出す）。 */
    private static int rerolls;

    private EnchantSpot() {
    }

    private static boolean enabled() {
        return KindSwitches.isEnabled(HitKind.ENCHANT) && ClientSettings.get().enchantWeakSpotEnabled
                && Minecraft.getMinecraft().player != null && !Minecraft.getMinecraft().player.isSpectator();
    }

    /** 台に物が置いてあり、候補が 1 つ以上出ているか。 */
    private static boolean hasOffers(GuiEnchantment gui) {
        ContainerEnchantment container = (ContainerEnchantment) gui.inventorySlots;
        if (container.tableInventory.getStackInSlot(0).isEmpty()) {
            return false;
        }
        for (int level : container.enchantLevels) {
            if (level > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasItem(GuiEnchantment gui) {
        return !((ContainerEnchantment) gui.inventorySlots).tableInventory.getStackInSlot(0).isEmpty();
    }

    /** 画面が変わったら（開き直したら）、回数を 0 に戻して、新しい位置に出す。 */
    private static void ensure(GuiScreen gui) {
        if (shownOn == gui) {
            return;
        }
        shownOn = gui;
        rerolls = 0;
        has = place(gui, gui.width / 2.0, gui.height / 2.0);
        MOTION.jumpTo(x, y);
    }

    /**
     * 枠の外のランダムな位置。前の位置から画面の高さの 1/4 以上離す（取れなければ、一番離れた位置）。
     * 置ける場所がなければ false。
     */
    private static boolean place(GuiScreen gui, double prevX, double prevY) {
        double left = (gui.width - PANEL_WIDTH) / 2.0 - PANEL_GAP - RADIUS;
        double right = (gui.width + PANEL_WIDTH) / 2.0 + PANEL_GAP + RADIUS;
        double top = (gui.height - PANEL_HEIGHT) / 2.0 - PANEL_GAP - RADIUS;
        double bottom = (gui.height + PANEL_HEIGHT) / 2.0 + PANEL_GAP + RADIUS;
        double minX = EDGE + RADIUS;
        double maxX = gui.width - EDGE - RADIUS;
        double minY = EDGE + RADIUS;
        double maxY = gui.height - EDGE - RADIUS;
        if (maxX <= minX || maxY <= minY) {
            return false;
        }
        double minMove = gui.height / 4.0;
        boolean found = false;
        double bestX = 0;
        double bestY = 0;
        double bestMove = -1;
        for (int i = 0; i < 80; i++) {
            double nx = minX + RANDOM.nextDouble() * (maxX - minX);
            double ny = minY + RANDOM.nextDouble() * (maxY - minY);
            if (nx > left && nx < right && ny > top && ny < bottom) {
                continue;
            }
            double move = Math.hypot(nx - prevX, ny - prevY);
            if (move >= minMove) {
                x = nx;
                y = ny;
                return true;
            }
            if (move > bestMove) {
                found = true;
                bestMove = move;
                bestX = nx;
                bestY = ny;
            }
        }
        if (found) {
            x = bestX;
            y = bestY;
        }
        return found;
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();
        if (!(screen instanceof GuiEnchantment) || !enabled()) {
            shownOn = null;
            return;
        }
        GuiEnchantment gui = (GuiEnchantment) screen;
        ensure(gui);
        Minecraft mc = Minecraft.getMinecraft();
        int rgb = MarkerLook.color(HitKind.ENCHANT, RGB);
        boolean offers = hasOffers(gui);
        if (offers && has) {
            long nowMs = Minecraft.getSystemTime();
            boolean trail = WeakSpotConfig.weakSpotTrailEnabled;
            MarkerShape shape = MarkerLook.shape(HitKind.ENCHANT);
            HudSpot.beginOverlay();
            if (trail) {
                for (MarkerMotion.Afterimage image : MOTION.afterimages(nowMs)) {
                    ScreenProjection.afterimage(image.u, image.v, RADIUS, shape, rgb, (float) image.alpha(nowMs));
                }
            }
            double[] p = trail ? MOTION.position(nowMs) : new double[] {x, y};
            ScreenProjection.marker(p[0], p[1], RADIUS, shape, rgb, 0.55F, 1);
            HudSpot.endOverlay();
        }
        String hint;
        if (!hasItem(gui)) {
            hint = I18n.format("weakspot.enchant.hintEmpty");
        } else if (rerolls > 0) {
            hint = I18n.format("weakspot.enchant.hintCount", rerolls);
        } else {
            hint = I18n.format("weakspot.enchant.hint");
        }
        int top = (gui.height - PANEL_HEIGHT) / 2;
        mc.fontRenderer.drawStringWithShadow(hint, (gui.width - mc.fontRenderer.getStringWidth(hint)) / 2.0F,
                top - 11, rgb);
    }

    @SubscribeEvent
    public static void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        GuiScreen screen = event.getGui();
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState() || !(screen instanceof GuiEnchantment)
                || !enabled() || shownOn != screen || !has || !hasOffers((GuiEnchantment) screen)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        double mouseX = Mouse.getEventX() * screen.width / (double) mc.displayWidth;
        double mouseY = screen.height - Mouse.getEventY() * screen.height / (double) mc.displayHeight - 1;
        if (Math.hypot(mouseX - x, mouseY - y) > RADIUS) {
            return;
        }
        // 当てたクリックは、スロットや候補に届かないようにする
        event.setCanceled(true);
        if (!ClientWeakSpotHandler.canHitNow(HitKind.ENCHANT, ClientSettings.get().enchantMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.ENCHANT);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.ENCHANT, streak));
        rerolls++;
        if (place(screen, x, y)) {
            if (WeakSpotConfig.weakSpotTrailEnabled) {
                MOTION.moveTo(x, y, Minecraft.getSystemTime());
            } else {
                MOTION.jumpTo(x, y);
            }
        }
    }
}

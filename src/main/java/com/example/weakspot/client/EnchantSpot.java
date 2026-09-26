package com.example.weakspot.client;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerColor;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerEnchantment;

/**
 * エンチャントの弱点（1.7.0）。エンチャント台の画面で、候補が出ている間、画面の枠の外に紫のマーカーを出し、クリックで
 * 当てる（寝ている間の弱点と同じ。描く・当てる流れは ScreenSpots。1.8.8）。当てると、サーバーが 3 つの候補を引き直す
 * （EnchantHits。何も減らない）。枠の上に、引き直せることと回数の注釈を出す。
 */
final class EnchantSpot extends ScreenSpotKind {

    /** 紫 #B070FF。 */
    private static final int RGB = 0xB070FF;
    /** エンチャント台の画面の枠の大きさ（GuiEnchantment の xSize / ySize）。 */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    /** 枠から離す距離と、画面の端から内側に入れる距離（GUI ピクセル）。 */
    private static final int PANEL_GAP = 8;
    private static final int EDGE = 16;

    /** この画面を開いてから引き直した回数（注釈に出す）。 */
    private int rerolls;

    EnchantSpot() {
        super(HitKind.ENCHANT);
    }

    @Override
    boolean eligible(Minecraft mc, GuiScreen gui) {
        return gui instanceof GuiEnchantment && KindSwitches.isEnabled(HitKind.ENCHANT)
                && ClientSettings.get().enchantWeakSpotEnabled && mc.player != null && !mc.player.isSpectator();
    }

    @Override
    boolean markerVisible(GuiScreen gui) {
        return has && hasOffers((GuiEnchantment) gui);
    }

    @Override
    int minHitInterval(SyncedSettings settings) {
        return settings.enchantMinHitIntervalTicks;
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

    @Override
    void onShown() {
        rerolls = 0;
    }

    @Override
    void onHit() {
        rerolls++;
    }

    /**
     * 枠の外のランダムな位置。前の位置から画面の高さの 1/4 以上離す（取れなければ、一番離れた位置）。
     * 置ける場所がなければ false。
     */
    @Override
    boolean place(GuiScreen gui, double prevX, double prevY) {
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
            double nx = minX + random.nextDouble() * (maxX - minX);
            double ny = minY + random.nextDouble() * (maxY - minY);
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

    @Override
    float[][] look() {
        int rgb = MarkerLook.color(HitKind.ENCHANT, RGB);
        return new float[][] {MarkerColor.towardWhite(rgb, 0), MarkerColor.towardWhite(rgb, 0.5F),
                MarkerColor.towardWhite(rgb, 0.8F)};
    }

    @Override
    float outlineAlpha() {
        return 0.9F;
    }

    /** 枠の上の注釈（台が空・まだ引き直していない・引き直した回数）。 */
    @Override
    void drawExtra(Minecraft mc, GuiScreen gui) {
        String hint;
        if (!hasItem((GuiEnchantment) gui)) {
            hint = I18n.format("weakspot.enchant.hintEmpty");
        } else if (rerolls > 0) {
            hint = I18n.format("weakspot.enchant.hintCount", rerolls);
        } else {
            hint = I18n.format("weakspot.enchant.hint");
        }
        int top = (gui.height - PANEL_HEIGHT) / 2;
        mc.fontRenderer.drawStringWithShadow(hint, (gui.width - mc.fontRenderer.getStringWidth(hint)) / 2.0F,
                top - 11, MarkerLook.color(HitKind.ENCHANT, RGB));
    }
}

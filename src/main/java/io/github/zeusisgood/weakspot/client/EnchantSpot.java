package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerEnchantment;

/** 当てると、サーバーが 3 つの候補を引き直す（EnchantHits。何も減らない）。 */
final class EnchantSpot extends ScreenSpotKind {

    /** GuiEnchantment の xSize / ySize。 */
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    private static final int HINT_BACKGROUND = 0x80000000;
    /** 枠から離す距離と、画面の端から内側に入れる距離（GUI ピクセル）。 */
    private static final int PANEL_GAP = 8;
    private static final int EDGE = 16;

    EnchantSpot() {
        super(HitKind.ENCHANT);
    }

    @Override
    boolean eligible(Minecraft mc, GuiScreen gui) {
        return gui instanceof GuiEnchantment && KindSwitches.isEnabled(HitKind.ENCHANT)
                && ClientSettings.get().enabled(HitKind.ENCHANT) && mc.player != null && !mc.player.isSpectator();
    }

    @Override
    boolean markerVisible(GuiScreen gui) {
        return has && hasOffers((GuiEnchantment) gui);
    }

    private static boolean hasOffers(GuiEnchantment gui) {
        if (!hasItem(gui)) {
            return false;
        }
        for (int level : ((ContainerEnchantment) gui.inventorySlots).enchantLevels) {
            if (level > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasItem(GuiEnchantment gui) {
        return !((ContainerEnchantment) gui.inventorySlots).tableInventory.getStackInSlot(0).isEmpty();
    }

    /** 前の位置から画面の高さの 1/4 以上離す。置ける場所がなければ false。 */
    @Override
    boolean place(GuiScreen gui, double prevX, double prevY) {
        double[] panel = {
                (gui.width - PANEL_WIDTH) / 2.0 - PANEL_GAP - radius(),
                (gui.width + PANEL_WIDTH) / 2.0 + PANEL_GAP + radius(),
                (gui.height - PANEL_HEIGHT) / 2.0 - PANEL_GAP - radius(),
                (gui.height + PANEL_HEIGHT) / 2.0 + PANEL_GAP + radius()};
        double minX = EDGE + radius();
        double maxX = gui.width - EDGE - radius();
        double minY = EDGE + radius();
        double maxY = gui.height - EDGE - radius();
        if (maxX <= minX || maxY <= minY) {
            return false;
        }
        return placeRandom(minX, maxX, minY, maxY, panel, 80, gui.height / 4.0, prevX, prevY);
    }

    @Override
    float[][] look() {
        return ScreenProjection.lookOf(MarkerLook.color(HitKind.ENCHANT));
    }

    @Override
    float outlineAlpha() {
        return 0.9F;
    }

    @Override
    void drawExtra(Minecraft mc, GuiScreen gui) {
        String hint = I18n.format(hasItem((GuiEnchantment) gui) ? "weakspot.enchant.hint" : "weakspot.enchant.hintEmpty");
        int width = mc.fontRenderer.getStringWidth(hint);
        int x = (gui.width - width) / 2;
        int y = (gui.height - PANEL_HEIGHT) / 2 - 11;
        // どの背景でも読めるように、黒の半透明の帯の上に描く
        Gui.drawRect(x - 2, y - 1, x + width + 2, y + mc.fontRenderer.FONT_HEIGHT, HINT_BACKGROUND);
        mc.fontRenderer.drawStringWithShadow(hint, x, y, MarkerLook.color(HitKind.ENCHANT));
    }
}

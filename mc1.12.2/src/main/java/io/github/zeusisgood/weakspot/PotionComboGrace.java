package io.github.zeusisgood.weakspot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 付いている間、コンボが途切れるまでの時間が延びる。見るのは両側の HitStreak の持ち主（Trinkets.comboResetTicks）。 */
public final class PotionComboGrace extends Potion {

    private static final ResourceLocation ICON = new ResourceLocation(WeakSpotMod.MODID,
            "textures/mob_effect/combo_grace.png");
    private static final int ICON_SIZE = 18;

    PotionComboGrace() {
        super(false, 0xFFD23F);
        setRegistryName(WeakSpotMod.MODID, "combo_grace");
        setPotionName("effect." + WeakSpotMod.MODID + ".combo_grace");
        setBeneficial();
    }

    /** バニラのアイコンの表（inventory.png）は使わず、自分の絵を描く。 */
    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        drawIcon(mc, x + 6, y + 7, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        drawIcon(mc, x + 3, y + 3, alpha);
    }

    @SideOnly(Side.CLIENT)
    private static void drawIcon(Minecraft mc, int x, int y, float alpha) {
        mc.getTextureManager().bindTexture(ICON);
        GlStateManager.color(1, 1, 1, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        GlStateManager.color(1, 1, 1, 1);
    }
}

package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

/**
 * Mods メニューの「設定」。中身は Forge が @Config から作る画面のままで、
 * 他人のサーバーに接続している間だけ、ここでの値が使われない旨を2行目に出す。
 */
public final class WeakSpotGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return create(parentScreen);
    }

    /** 統計画面の「設定画面を開く」からも使う。 */
    static GuiConfig create(GuiScreen parentScreen) {
        GuiConfig gui = new GuiConfig(parentScreen, WeakSpotMod.MODID, WeakSpotMod.NAME);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null && !mc.isSingleplayer()) {
            gui.titleLine2 = I18n.format("weakspot.config.remoteNotice");
        }
        return gui;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}

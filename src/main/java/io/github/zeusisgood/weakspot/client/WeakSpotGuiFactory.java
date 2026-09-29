package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.SettingGroups;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

/**
 * Mods メニューの「設定」。1.10.2 から、ファイルのカテゴリ（server / client）ではなく、種類・用途のまとめ（SettingGroups）で
 * 組む。項目そのもの（Property）は ConfigManager の Configuration のものを使うので、値の保存と反映は Forge の画面と同じ流れ
 * （OnConfigChangedEvent → WeakSpotConfig.Sync）。他人のサーバーに接続している間は、サーバーの値が優先される旨を2行目に出す。
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
        List<IConfigElement> elements = elements();
        GuiConfig gui = elements.isEmpty()
                ? new GuiConfig(parentScreen, WeakSpotMod.MODID, WeakSpotMod.NAME)
                : new GuiConfig(parentScreen, elements, WeakSpotMod.MODID, false, false, WeakSpotMod.NAME);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null && !mc.isSingleplayer()) {
            gui.titleLine2 = I18n.format("weakspot.config.remoteNotice");
        }
        return gui;
    }

    /** まとめごとの項目。設定ファイルが取り出せなければ空（そのときは Forge の既定の画面）。 */
    private static List<IConfigElement> elements() {
        List<IConfigElement> groups = new ArrayList<>();
        Configuration cfg = WeakSpotConfig.configuration();
        if (cfg == null) {
            return groups;
        }
        for (Map.Entry<String, List<String>> group : SettingGroups.GROUPS.entrySet()) {
            List<IConfigElement> children = new ArrayList<>();
            for (String name : group.getValue()) {
                WeakSpotConfig.Setting setting = WeakSpotConfig.setting(name);
                Property property = setting == null ? null : cfg.getCategory(setting.category).get(name);
                if (property != null) {
                    children.add(new ConfigElement(property));
                }
            }
            if (!children.isEmpty()) {
                groups.add(new DummyConfigElement.DummyCategoryElement(group.getKey(),
                        SettingGroups.langKey(group.getKey()), children));
            }
        }
        return groups;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}

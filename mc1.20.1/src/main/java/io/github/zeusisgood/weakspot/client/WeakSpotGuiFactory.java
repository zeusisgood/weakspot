package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.SettingGroups;
import io.github.zeusisgood.weakspot.config.Config;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 設定画面（Mod の一覧の「設定」と、統計画面の「設定画面を開く」）。ファイルのカテゴリ（server / client）ではなく、
 * 種類・用途のまとめ（SettingGroups）で組む（1.12.2 版と同じ並び）。Forge の標準の設定画面は、ファイルの
 * カテゴリのままの並びになるので使わない。値は「完了」でフィールドに書き、ファイルへ保存する（サーバーの項目も）。
 */
public final class WeakSpotGuiFactory {

    private WeakSpotGuiFactory() {
    }

    public static Screen createConfigGui(Screen parent) {
        return create(parent);
    }

    static Screen create(Screen parent) {
        return new GroupsScreen(parent);
    }

    private static boolean remote() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && !mc.isSingleplayer();
    }

    /** まとめの一覧。 */
    private static final class GroupsScreen extends Screen {

        private static final int BUTTON_WIDTH = 150;

        private final Screen parent;

        GroupsScreen(Screen parent) {
            super(Component.literal(WeakSpotMod.NAME));
            this.parent = parent;
        }

        @Override
        protected void init() {
            List<String> groups = new ArrayList<>();
            for (Map.Entry<String, List<String>> group : SettingGroups.GROUPS.entrySet()) {
                if (!settingsOf(group.getValue()).isEmpty()) {
                    groups.add(group.getKey());
                }
            }
            int rows = (groups.size() + 1) / 2;
            int top = Math.max(40, height / 2 - rows * 12);
            for (int i = 0; i < groups.size(); i++) {
                String group = groups.get(i);
                int x = width / 2 + (i % 2 == 0 ? -BUTTON_WIDTH - 4 : 4);
                int y = top + (i / 2) * 24;
                addRenderableWidget(Button.builder(Component.translatable(SettingGroups.langKey(group)),
                        b -> minecraft.setScreen(new SettingsScreen(this, group))).bounds(x, y, BUTTON_WIDTH, 20)
                        .tooltip(Tooltip.create(Component.translatable(SettingGroups.langKey(group) + ".tooltip")))
                        .build());
            }
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(width / 2 - 100, Math.min(height - 28, top + rows * 24 + 12), 200, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
            if (remote()) {
                g.drawCenteredString(font, I18n.get("weakspot.config.remoteNotice"), width / 2, 24, 0xFFFFFF55);
            }
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }
    }

    private static List<WeakSpotConfig.Setting> settingsOf(List<String> names) {
        List<WeakSpotConfig.Setting> list = new ArrayList<>();
        for (String name : names) {
            WeakSpotConfig.Setting setting = WeakSpotConfig.setting(name);
            if (setting != null) {
                list.add(setting);
            }
        }
        return list;
    }

    /** 1 つのまとめの項目。多ければページに分ける。 */
    private static final class SettingsScreen extends Screen {

        private static final int ROW_HEIGHT = 24;
        private static final int WIDGET_WIDTH = 150;
        private static final int MAX_WIDTH = 420;
        private static final int ERROR_RGB = 0xFF5555;

        private final Screen parent;
        private final List<WeakSpotConfig.Setting> settings;
        /** 編集中の値（フィールドと同じ型）。完了で書く。 */
        private final Object[] values;
        private final boolean[] invalid;
        private int page;
        private int perPage;
        private int left;
        private int right;
        private int top;

        SettingsScreen(Screen parent, String group) {
            super(Component.translatable(SettingGroups.langKey(group)));
            this.parent = parent;
            this.settings = settingsOf(SettingGroups.GROUPS.get(group));
            this.values = new Object[settings.size()];
            this.invalid = new boolean[settings.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = copy(settings.get(i).get());
            }
        }

        @Override
        protected void init() {
            clearWidgets();
            int width = Math.min(MAX_WIDTH, this.width - 20);
            left = this.width / 2 - width / 2;
            right = left + width;
            top = 40;
            perPage = Math.max(1, (height - top - 40) / ROW_HEIGHT);
            int pages = (settings.size() + perPage - 1) / perPage;
            page = Math.min(page, Math.max(0, pages - 1));
            for (int row = 0; row < perPage; row++) {
                int i = page * perPage + row;
                if (i >= settings.size()) {
                    break;
                }
                addWidget(i, right - WIDGET_WIDTH, top + row * ROW_HEIGHT);
            }
            int y = height - 28;
            if (pages > 1) {
                addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
                        .bounds(this.width / 2 - 154, y, 20, 20).build()).active = page > 0;
                addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
                        .bounds(this.width / 2 + 134, y, 20, 20).build()).active = page < pages - 1;
            }
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(this.width / 2 - 100, y, 200, 20).build());
        }

        private void turn(int delta) {
            page += delta;
            init();
        }

        private void addWidget(int i, int x, int y) {
            WeakSpotConfig.Setting setting = settings.get(i);
            Tooltip tooltip = Tooltip.create(Component.translatable(setting.tooltipKey()));
            Class<?> type = setting.field.getType();
            if (type == boolean.class) {
                addRenderableWidget(Button.builder(onOff((Boolean) values[i]), b -> {
                    values[i] = !(Boolean) values[i];
                    b.setMessage(onOff((Boolean) values[i]));
                }).bounds(x, y, WIDGET_WIDTH, 20).tooltip(tooltip).build());
                return;
            }
            if (type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                addRenderableWidget(Button.builder(Component.literal(values[i].toString()), b -> {
                    int next = (Arrays.asList(constants).indexOf(values[i]) + 1) % constants.length;
                    values[i] = constants[next];
                    b.setMessage(Component.literal(values[i].toString()));
                }).bounds(x, y, WIDGET_WIDTH, 20).tooltip(tooltip).build());
                return;
            }
            EditBox box = new EditBox(font, x + 1, y + 1, WIDGET_WIDTH - 2, 18, Component.translatable(setting.nameKey()));
            box.setMaxLength(4096);
            box.setValue(toText(values[i]));
            box.setTooltip(tooltip);
            box.setResponder(text -> {
                Object parsed = parse(setting, text);
                invalid[i] = parsed == null;
                box.setTextColor(invalid[i] ? ERROR_RGB : 0xE0E0E0);
                if (parsed != null) {
                    values[i] = parsed;
                }
            });
            addRenderableWidget(box);
        }

        private static Component onOff(boolean on) {
            return on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
            if (remote()) {
                g.drawCenteredString(font, I18n.get("weakspot.config.remoteNotice"), width / 2, 24, 0xFFFFFF55);
            }
            int labelWidth = right - WIDGET_WIDTH - 6 - left;
            for (int row = 0; row < perPage; row++) {
                int i = page * perPage + row;
                if (i >= settings.size()) {
                    break;
                }
                String label = I18n.get(settings.get(i).nameKey());
                if (font.width(label) > labelWidth) {
                    label = font.plainSubstrByWidth(label, labelWidth - font.width("…")) + "…";
                }
                g.drawString(font, label, left, top + row * ROW_HEIGHT + 6, invalid[i] ? 0xFF000000 | ERROR_RGB
                        : 0xFFFFFFFF);
            }
        }

        /** 読めない値の項目は、変えずに残す。 */
        @Override
        public void onClose() {
            boolean client = false;
            boolean server = false;
            for (int i = 0; i < settings.size(); i++) {
                WeakSpotConfig.Setting setting = settings.get(i);
                if (invalid[i] || same(setting.get(), values[i])) {
                    continue;
                }
                setting.set(values[i]);
                if (setting.category.startsWith("server")) {
                    server = true;
                } else {
                    client = true;
                }
            }
            if (client) {
                WeakSpotConfig.save();
            }
            if (server) {
                WeakSpotConfig.saveServer();
            }
            minecraft.setScreen(parent);
        }
    }

    private static Object copy(Object value) {
        if (value instanceof String[] array) {
            return array.clone();
        }
        if (value instanceof int[] array) {
            return array.clone();
        }
        return value;
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof String[] x && b instanceof String[] y) {
            return Arrays.equals(x, y);
        }
        if (a instanceof int[] x && b instanceof int[] y) {
            return Arrays.equals(x, y);
        }
        return a.equals(b);
    }

    /** 一覧は「,」で区切る。 */
    private static String toText(Object value) {
        if (value instanceof String[] array) {
            return String.join(", ", array);
        }
        if (value instanceof int[] array) {
            StringBuilder sb = new StringBuilder();
            for (int v : array) {
                sb.append(sb.length() == 0 ? "" : ", ").append(v);
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    /** 読めない・範囲の外なら null。 */
    private static Object parse(WeakSpotConfig.Setting setting, String text) {
        Class<?> type = setting.field.getType();
        String t = text.trim();
        try {
            if (type == int.class) {
                int v = Integer.parseInt(t);
                Config.RangeInt range = setting.field.getAnnotation(Config.RangeInt.class);
                return range != null && (v < range.min() || v > range.max()) ? null : v;
            }
            if (type == double.class) {
                double v = Double.parseDouble(t);
                Config.RangeDouble range = setting.field.getAnnotation(Config.RangeDouble.class);
                return Double.isNaN(v) || range != null && (v < range.min() || v > range.max()) ? null : v;
            }
            if (type == String[].class) {
                return t.isEmpty() ? new String[0] : split(t).toArray(new String[0]);
            }
            if (type == int[].class) {
                List<String> parts = split(t);
                int[] array = new int[parts.size()];
                for (int i = 0; i < array.length; i++) {
                    array[i] = Integer.parseInt(parts.get(i));
                }
                return array;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return text;
    }

    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        if (text.isEmpty()) {
            return parts;
        }
        for (String part : text.split(",")) {
            if (!part.trim().isEmpty()) {
                parts.add(part.trim());
            }
        }
        return parts;
    }
}

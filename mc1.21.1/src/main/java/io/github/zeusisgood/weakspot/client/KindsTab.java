package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerColor;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** 入りきらない行はホイールで送る。 */
final class KindsTab extends StatsScreenTab {

    private static final int KIND_ROW_HEIGHT = 22;

    private static final int KINDS = HitKind.values().length;
    private final Button[] toggleButtons = new Button[KINDS];
    private final Button[] shapeButtons = new Button[KINDS];
    /** 行ごとの、オン/オフと形の名前以外のボタン（◀ ▶）。サーバーでオフの種類では隠す。 */
    private final List<List<Button>> arrowButtons = new ArrayList<>();
    private final EditBox[] colorFields = new EditBox[KINDS];
    private final boolean[] colorInvalid = new boolean[KINDS];
    private boolean shown;
    private int scroll;
    /** ◀ ▶ で入力欄を書き換えている間は、書き換えた文字を色として読み直さない。 */
    private boolean settingField;

    KindsTab(StatsScreen screen) {
        super(screen);
    }

    @Override
    void init() {
        arrowButtons.clear();
        int center = screen.width / 2;
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            // 1 行: [名前][オン/オフ][■][◀][入力欄][▶] [◀][形][▶]（幅は center ± 154 に収める）
            toggleButtons[i] = screen.add(button("", center - 64, 36, b -> {
                KindSwitches.toggle(kind);
                updateLabels();
            }));
            List<Button> arrows = new ArrayList<>();
            arrows.add(screen.add(button("◀", center - 14, 12, b -> stepColor(kind, -1))));
            arrows.add(screen.add(button("▶", center + 50, 12, b -> stepColor(kind, 1))));
            arrows.add(screen.add(button("◀", center + 66, 12, b -> stepShape(kind, -1))));
            shapeButtons[i] = screen.add(button("", center + 80, 54, b -> stepShape(kind, 1)));
            arrows.add(screen.add(button("▶", center + 136, 12, b -> stepShape(kind, 1))));
            arrowButtons.add(arrows);
            EditBox field = new EditBox(screen.font(), center, 0, 48, 18, Component.empty());
            field.setMaxLength(7);
            field.setValue(colorText(kind));
            field.setResponder(text -> {
                if (!settingField) {
                    applyTypedColor(kind);
                }
            });
            colorFields[i] = screen.addInput(field);
            colorInvalid[i] = false;
        }
    }

    private static Button button(String text, int x, int width, Button.OnPress onPress) {
        return Button.builder(Component.literal(text), onPress).bounds(x, 0, width, 20).build();
    }

    @Override
    void show(boolean shown) {
        this.shown = shown;
        layoutRows();
    }

    private int visibleRows() {
        return Math.max(1, (screen.bottom() - 4 - (screen.top() + 52)) / KIND_ROW_HEIGHT);
    }

    /** 見えない行は隠す。 */
    private void layoutRows() {
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, KINDS - rows));
        SyncedSettings settings = ClientSettings.get();
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            int row = i - scroll;
            boolean visible = shown && row >= 0 && row < rows;
            int y = screen.top() + 52 + row * KIND_ROW_HEIGHT;
            boolean server = settings.enabled(kind);
            toggleButtons[i].visible = visible;
            toggleButtons[i].setY(y);
            toggleButtons[i].active = server;
            shapeButtons[i].visible = visible && server;
            shapeButtons[i].setY(y);
            for (Button button : arrowButtons.get(i)) {
                button.visible = visible && server;
                button.setY(y);
            }
            colorFields[i].setY(y + 1);
            colorFields[i].setVisible(visible && server);
            colorFields[i].setEditable(server);
            if (!colorFields[i].isVisible() && colorFields[i].isFocused()) {
                screen.setFocused(null);
            }
        }
        updateLabels();
    }

    private void updateLabels() {
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            toggleButtons[i].setMessage(Component.literal(I18n.get(KindSwitches.isDisabledByPlayer(kind)
                    ? "weakspot.kinds.off" : "weakspot.kinds.on")));
            shapeButtons[i].setMessage(Component.literal(I18n.get("weakspot.kinds.shape."
                    + MarkerLook.shape(kind).name().toLowerCase(Locale.ROOT))));
        }
    }

    /** 今の色が輪にないとき（打ち込んだ色）は、▶ で最初、◀ で最後の色にする。 */
    private void stepColor(HitKind kind, int direction) {
        List<Object> ring = new ArrayList<>();
        ring.add(null);
        for (int rgb : MarkerLook.PRESETS) {
            ring.add(rgb);
        }
        for (String special : new String[] {MarkerLook.SILVER, MarkerLook.RAINBOW}) {
            if (MarkerLook.specialUnlocked(special)) {
                ring.add(special);
            }
        }
        String special = MarkerLook.special(kind);
        Object current = special != null && MarkerLook.specialUnlocked(special) ? special : MarkerLook.customColor(kind);
        int position = ring.indexOf(current);
        int next = position < 0 ? (direction > 0 ? 1 : ring.size() - 1)
                : Math.floorMod(position + direction, ring.size());
        Object chosen = ring.get(next);
        if (chosen instanceof String) {
            MarkerLook.setSpecial(kind, (String) chosen);
        } else {
            MarkerLook.setColor(kind, (Integer) chosen);
        }
        int i = kind.ordinal();
        settingField = true;
        colorFields[i].setValue(colorText(kind));
        settingField = false;
        colorInvalid[i] = false;
    }

    /** 特別な色は名前、書き換えていなければ空。 */
    private static String colorText(HitKind kind) {
        String special = MarkerLook.special(kind);
        if (special != null && MarkerLook.specialUnlocked(special)) {
            return special;
        }
        Integer custom = MarkerLook.customColor(kind);
        return custom == null ? "" : String.format("#%06X", custom);
    }

    private void stepShape(HitKind kind, int direction) {
        MarkerShape[] shapes = MarkerShape.values();
        int next = MarkerLook.shape(kind).ordinal();
        do {
            next = Math.floorMod(next + direction, shapes.length);
        } while (shapes[next] == MarkerShape.STAR && !TargetRecords.unlocked(TargetRules.Tier.BRONZE));
        MarkerLook.setShape(kind, shapes[next]);
        updateLabels();
    }

    /** 空なら初期値に戻す。読めなければ受け付けず、枠を赤くする。 */
    private void applyTypedColor(HitKind kind) {
        int i = kind.ordinal();
        String text = colorFields[i].getValue().trim();
        if (text.isEmpty()) {
            MarkerLook.setColor(kind, null);
            colorInvalid[i] = false;
            return;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if ((MarkerLook.SILVER.equals(lower) || MarkerLook.RAINBOW.equals(lower)) && MarkerLook.specialUnlocked(lower)) {
            MarkerLook.setSpecial(kind, lower);
            colorInvalid[i] = false;
            return;
        }
        int rgb = MarkerColor.parse(text, -1);
        colorInvalid[i] = rgb < 0;
        if (rgb >= 0) {
            MarkerLook.setColor(kind, rgb);
        }
    }

    /** 入力中の Esc は画面を閉じずに入力を終える。文字そのものは画面が入力欄へ渡す。 */
    @Override
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox field : colorFields) {
            if (field.isVisible() && field.isFocused()) {
                if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_RETURN
                        || keyCode == InputConstants.KEY_NUMPADENTER) {
                    screen.setFocused(null);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    @Override
    void scroll(int step) {
        scroll += step;
        layoutRows();
    }

    @Override
    void draw(GuiGraphics g) {
        int center = screen.width / 2;
        int top = screen.top();
        screen.drawString(g, I18n.get("weakspot.kinds.column.kind"), center - 154, top + 40, 0xAAAAAA);
        screen.drawString(g, I18n.get("weakspot.kinds.column.onOff"), center - 64, top + 40, 0xAAAAAA);
        screen.drawString(g, I18n.get("weakspot.kinds.column.color"), center - 26, top + 40, 0xAAAAAA);
        screen.drawString(g, I18n.get("weakspot.kinds.column.shape"), center + 66, top + 40, 0xAAAAAA);
        if (!WeakSpotConfig.client.markers.weakSpotsEnabled) {
            // 列の見出しと重ならないように、題の行の右に出す
            screen.drawRight(g, I18n.get("weakspot.kinds.pausedAll", ToggleKeyHandler.keyName()), center + 154, top,
                    0xFFFF55);
        }
        SyncedSettings settings = ClientSettings.get();
        int rows = visibleRows();
        for (HitKind kind : HitKind.values()) {
            int row = kind.ordinal() - scroll;
            if (row < 0 || row >= rows) {
                continue;
            }
            int y = top + 52 + row * KIND_ROW_HEIGHT;
            boolean server = settings.enabled(kind);
            screen.drawString(g, I18n.get("weakspot.kind." + kind.key()), center - 154, y + 6,
                    server ? 0xFFFFFF : 0x808080);
            if (!server) {
                screen.drawString(g, I18n.get("weakspot.kinds.serverDisabled"), center - 26, y + 6, 0x808080);
                continue;
            }
            int rgb = MarkerLook.color(kind);
            g.fill(center - 26, y + 5, center - 16, y + 15, 0xFF000000 | rgb);
            EditBox field = colorFields[kind.ordinal()];
            field.setX(center);
            field.render(g, screen.mouseX(), screen.mouseY(), 0);
            if (colorInvalid[kind.ordinal()]) {
                int x0 = field.getX() - 1;
                int y0 = field.getY() - 1;
                int x1 = x0 + field.getWidth() + 1;
                int y1 = y0 + field.getHeight() + 1;
                g.fill(x0, y0, x1 + 1, y0 + 1, 0xFFFF4D4D);
                g.fill(x0, y1, x1 + 1, y1 + 1, 0xFFFF4D4D);
                g.fill(x0, y0, x0 + 1, y1 + 1, 0xFFFF4D4D);
                g.fill(x1, y0, x1 + 1, y1 + 1, 0xFFFF4D4D);
            } else if (field.getValue().isEmpty() && !field.isFocused()) {
                screen.drawString(g, I18n.get("weakspot.kinds.default"), field.getX() + 4, field.getY() + 5, 0x808080);
            }
        }
        screen.drawScrollHint(g, scroll > 0, scroll + rows < KINDS);
    }
}

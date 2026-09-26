package com.example.weakspot.client;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerColor;
import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

/**
 * 統計画面の「弱点マーカー」タブ（1.7.0。1.8.3 で「弱点」から改名し、色と形に ◀ を足した。1.8.9 で StatsScreen から
 * 分けた）。種類ごとのオン・オフ（KindSwitches。サーバーにも伝わる）と、自分の弱点の色と形（MarkerLook）。
 * 入りきらない行はホイールで送る。
 */
final class KindsTab extends StatsScreenTab {

    /** 行のボタン（+ 種類の番号）。 */
    private static final int BUTTON_KIND_TOGGLE = 100;
    private static final int BUTTON_KIND_COLOR = 200;
    private static final int BUTTON_KIND_SHAPE = 300;
    /** 色と形を 1 つ前に戻す ◀ と、形の ▶（1.8.3）。 */
    private static final int BUTTON_KIND_COLOR_PREV = 400;
    private static final int BUTTON_KIND_SHAPE_PREV = 500;
    private static final int BUTTON_KIND_SHAPE_NEXT = 600;
    private static final int KIND_ROW_HEIGHT = 22;

    private final List<GuiButton> buttons = new ArrayList<>();
    private final GuiTextField[] colorFields = new GuiTextField[HitKind.values().length];
    private final boolean[] colorInvalid = new boolean[HitKind.values().length];
    private boolean shown;
    private int scroll;

    KindsTab(StatsScreen screen) {
        super(screen);
    }

    @Override
    void init() {
        buttons.clear();
        int center = screen.width / 2;
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            // 1 行: [名前][オン/オフ][■][◀][入力欄][▶] [◀][形][▶]（1.8.3。幅は center ± 154 に収める）
            add(new GuiButton(BUTTON_KIND_TOGGLE + i, center - 64, 0, 36, 20, ""));
            add(new GuiButton(BUTTON_KIND_COLOR_PREV + i, center - 14, 0, 12, 20, "◀"));
            add(new GuiButton(BUTTON_KIND_COLOR + i, center + 50, 0, 12, 20, "▶"));
            add(new GuiButton(BUTTON_KIND_SHAPE_PREV + i, center + 66, 0, 12, 20, "◀"));
            add(new GuiButton(BUTTON_KIND_SHAPE + i, center + 80, 0, 54, 20, ""));
            add(new GuiButton(BUTTON_KIND_SHAPE_NEXT + i, center + 136, 0, 12, 20, "▶"));
            GuiTextField field = new GuiTextField(BUTTON_KIND_COLOR + 50 + i, screen.font(), center, 0, 48, 18);
            field.setMaxStringLength(7);
            Integer custom = MarkerLook.customColor(kind);
            field.setText(custom == null ? "" : String.format("#%06X", custom));
            colorFields[i] = field;
            colorInvalid[i] = false;
        }
    }

    private void add(GuiButton button) {
        buttons.add(button);
        screen.add(button);
    }

    @Override
    void show(boolean shown) {
        this.shown = shown;
        layoutRows();
    }

    /** 画面に入る行の数。 */
    private int visibleRows() {
        return Math.max(1, (screen.bottom() - 4 - (screen.top() + 52)) / KIND_ROW_HEIGHT);
    }

    /** 行のボタンを、今の送り位置に並べる（見えない行は隠す）。 */
    private void layoutRows() {
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, HitKind.values().length - rows));
        SyncedSettings settings = ClientSettings.get();
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            int row = i - scroll;
            boolean visible = shown && row >= 0 && row < rows;
            int y = screen.top() + 52 + row * KIND_ROW_HEIGHT;
            boolean server = settings.enabled(kind);
            for (GuiButton button : buttons) {
                if (button.id == BUTTON_KIND_TOGGLE + i) {
                    button.visible = visible;
                    button.y = y;
                    button.enabled = server;
                } else if (button.id == BUTTON_KIND_COLOR + i || button.id == BUTTON_KIND_SHAPE + i
                        || button.id == BUTTON_KIND_COLOR_PREV + i || button.id == BUTTON_KIND_SHAPE_PREV + i
                        || button.id == BUTTON_KIND_SHAPE_NEXT + i) {
                    // サーバーで無効な種類は、色と形を隠して「サーバーでオフ」と出す
                    button.visible = visible && server;
                    button.y = y;
                }
            }
            colorFields[i].y = y + 1;
            colorFields[i].setVisible(visible && server);
            colorFields[i].setEnabled(server);
        }
        updateLabels();
    }

    private void updateLabels() {
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            for (GuiButton button : buttons) {
                if (button.id == BUTTON_KIND_TOGGLE + i) {
                    button.displayString = I18n.format(KindSwitches.isDisabledByPlayer(kind)
                            ? "weakspot.kinds.off" : "weakspot.kinds.on");
                } else if (button.id == BUTTON_KIND_SHAPE + i) {
                    button.displayString = I18n.format("weakspot.kinds.shape."
                            + MarkerLook.shape(kind).name().toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
    }

    @Override
    boolean action(int id) {
        HitKind[] kinds = HitKind.values();
        if (id >= BUTTON_KIND_TOGGLE && id < BUTTON_KIND_TOGGLE + kinds.length) {
            KindSwitches.toggle(kinds[id - BUTTON_KIND_TOGGLE]);
            updateLabels();
        } else if (id >= BUTTON_KIND_COLOR && id < BUTTON_KIND_COLOR + kinds.length) {
            stepColor(kinds[id - BUTTON_KIND_COLOR], 1);
        } else if (id >= BUTTON_KIND_COLOR_PREV && id < BUTTON_KIND_COLOR_PREV + kinds.length) {
            stepColor(kinds[id - BUTTON_KIND_COLOR_PREV], -1);
        } else if (id >= BUTTON_KIND_SHAPE && id < BUTTON_KIND_SHAPE + kinds.length) {
            stepShape(kinds[id - BUTTON_KIND_SHAPE], 1);
        } else if (id >= BUTTON_KIND_SHAPE_NEXT && id < BUTTON_KIND_SHAPE_NEXT + kinds.length) {
            stepShape(kinds[id - BUTTON_KIND_SHAPE_NEXT], 1);
        } else if (id >= BUTTON_KIND_SHAPE_PREV && id < BUTTON_KIND_SHAPE_PREV + kinds.length) {
            stepShape(kinds[id - BUTTON_KIND_SHAPE_PREV], -1);
        } else {
            return false;
        }
        return true;
    }

    /**
     * ▶（direction = 1）で次、◀（-1）で前の色に切り替える。「初期値」→ 12 色 → 「初期値」の輪（1.8.3 で ◀ を足した）。
     * 今の色が 12 色にないとき（打ち込んだ色）は、▶ で最初、◀ で最後の色にする。
     */
    private void stepColor(HitKind kind, int direction) {
        Integer custom = MarkerLook.customColor(kind);
        int count = MarkerLook.PRESETS.length;
        // 輪の位置: 0 = 初期値、1〜count = 12 色
        int position = -1;
        if (custom == null) {
            position = 0;
        } else {
            for (int j = 0; j < count; j++) {
                if (MarkerLook.PRESETS[j] == custom) {
                    position = j + 1;
                }
            }
        }
        int next;
        if (position < 0) {
            next = direction > 0 ? 1 : count;
        } else {
            next = Math.floorMod(position + direction, count + 1);
        }
        Integer rgb = next == 0 ? null : Integer.valueOf(MarkerLook.PRESETS[next - 1]);
        MarkerLook.setColor(kind, rgb);
        int i = kind.ordinal();
        colorFields[i].setText(rgb == null ? "" : String.format("#%06X", rgb));
        colorInvalid[i] = false;
    }

    /** 形を、▶ で 円 → 輪 → ひし形 → 四角 → 円、◀ で逆に切り替える。 */
    private void stepShape(HitKind kind, int direction) {
        MarkerShape[] shapes = MarkerShape.values();
        int next = Math.floorMod(MarkerLook.shape(kind).ordinal() + direction, shapes.length);
        MarkerLook.setShape(kind, shapes[next]);
        updateLabels();
    }

    /** 入力欄のカラーコードを読む。空なら初期値に戻す。読めなければ受け付けず、枠を赤くする。 */
    private void applyTypedColor(HitKind kind) {
        int i = kind.ordinal();
        String text = colorFields[i].getText().trim();
        if (text.isEmpty()) {
            MarkerLook.setColor(kind, null);
            colorInvalid[i] = false;
            return;
        }
        int rgb = MarkerColor.parse(text, -1);
        colorInvalid[i] = rgb < 0;
        if (rgb >= 0) {
            MarkerLook.setColor(kind, rgb);
        }
    }

    @Override
    boolean keyTyped(char typedChar, int keyCode) {
        for (HitKind kind : HitKind.values()) {
            GuiTextField field = colorFields[kind.ordinal()];
            if (field.getVisible() && field.isFocused()) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    field.setFocused(false);
                    return true;
                }
                if (field.textboxKeyTyped(typedChar, keyCode)) {
                    applyTypedColor(kind);
                }
                if (keyCode == Keyboard.KEY_RETURN) {
                    field.setFocused(false);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (GuiTextField field : colorFields) {
            if (field.getVisible()) {
                field.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    void update() {
        for (GuiTextField field : colorFields) {
            field.updateCursorCounter();
        }
    }

    @Override
    void scroll(int step) {
        scroll += step;
        layoutRows();
    }

    @Override
    void draw() {
        int center = screen.width / 2;
        int top = screen.top();
        screen.drawString(screen.font(), I18n.format("weakspot.kinds.column.kind"), center - 154, top + 40, 0xAAAAAA);
        screen.drawString(screen.font(), I18n.format("weakspot.kinds.column.onOff"), center - 64, top + 40, 0xAAAAAA);
        screen.drawString(screen.font(), I18n.format("weakspot.kinds.column.color"), center - 26, top + 40, 0xAAAAAA);
        screen.drawString(screen.font(), I18n.format("weakspot.kinds.column.shape"), center + 66, top + 40, 0xAAAAAA);
        if (!WeakSpotConfig.weakSpotsEnabled) {
            // 題の行の右に出す（列の見出しと重ならないように）
            screen.drawRight(I18n.format("weakspot.kinds.pausedAll", ToggleKeyHandler.keyName()), center + 154, top,
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
            screen.drawString(screen.font(), I18n.format("weakspot.kind." + kind.key()), center - 154, y + 6,
                    server ? 0xFFFFFF : 0x808080);
            if (!server) {
                screen.drawString(screen.font(), I18n.format("weakspot.kinds.serverDisabled"), center - 26, y + 6,
                        0x808080);
                continue;
            }
            int rgb = MarkerLook.color(kind);
            // 見本（今の色）
            Gui.drawRect(center - 26, y + 5, center - 16, y + 15, 0xFF000000 | rgb);
            GuiTextField field = colorFields[kind.ordinal()];
            field.x = center;
            field.drawTextBox();
            if (colorInvalid[kind.ordinal()]) {
                int x0 = field.x - 1;
                int y0 = field.y - 1;
                int x1 = x0 + field.width + 1;
                int y1 = y0 + field.height + 1;
                Gui.drawRect(x0, y0, x1 + 1, y0 + 1, 0xFFFF4D4D);
                Gui.drawRect(x0, y1, x1 + 1, y1 + 1, 0xFFFF4D4D);
                Gui.drawRect(x0, y0, x0 + 1, y1 + 1, 0xFFFF4D4D);
                Gui.drawRect(x1, y0, x1 + 1, y1 + 1, 0xFFFF4D4D);
            } else if (field.getText().isEmpty() && !field.isFocused()) {
                screen.drawString(screen.font(), I18n.format("weakspot.kinds.default"), field.x + 4, field.y + 5,
                        0x808080);
            }
        }
        screen.drawScrollHint(scroll > 0, scroll + rows < HitKind.values().length);
    }
}

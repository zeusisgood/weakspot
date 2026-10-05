package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.config.ClientConfig;
import io.github.zeusisgood.weakspot.config.SoundPreset;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/**
 * マイプリセットの画面（1.11.1。「サウンド」タブの [マイプリセット…] から開く）。3 つの枠を中身付きで並べ、それぞれ
 * [読み込む] [保存]（保存済みの枠は 1 回目で「上書き？」、もう一度で上書き。3 秒かほかのボタンで戻る）。下に今の設定の中身。
 * 先例はバニラの保存したホットバーと、OptiFine のシェーダーのプロファイル。
 */
final class MyPresetsScreen extends GuiScreen {

    private static final int BUTTON_BACK = 0;
    private static final int BUTTON_LOAD = 10;
    private static final int BUTTON_SAVE = 20;
    private static final int ROW_HEIGHT = 26;
    private static final int BUTTON_WIDTH = 60;
    private static final int MAX_WIDTH = 400;
    /** 枠の名前の列の幅。 */
    private static final int NAME_WIDTH = 84;
    /** 保存しました・読み込みました（緑 #55FF55）と、上書き？（黄 #FFFF55）。 */
    private static final int DONE_RGB = 0x55FF55;
    private static final int CONFIRM_RGB = 0xFFFF55;
    private static final long DONE_MS = 2000;
    private static final long CONFIRM_MS = 3000;

    private final GuiScreen parent;
    private final GuiButton[] loads = new GuiButton[SoundPreset.MY_SLOTS];
    private final GuiButton[] saves = new GuiButton[SoundPreset.MY_SLOTS];
    private int left;
    private int right;
    private int top;
    /** 上書きの確かめ中の枠（1 始まり。0 はなし）と、その時刻。 */
    private int confirmSlot;
    private long confirmMs;
    /** 「保存しました」「読み込みました」を出している枠と、その文字の翻訳キー・時刻。 */
    private int doneSlot;
    private String doneKey;
    private long doneMs = Long.MIN_VALUE / 2;

    MyPresetsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int width = Math.min(MAX_WIDTH, this.width - 20);
        left = this.width / 2 - width / 2;
        right = left + width;
        top = Math.max(20, this.height / 2 - 70);
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            int y = top + 24 + i * ROW_HEIGHT;
            loads[i] = addButton(new GuiButton(BUTTON_LOAD + i, right - BUTTON_WIDTH * 2 - 4, y, BUTTON_WIDTH, 20,
                    I18n.format("weakspot.sound.preset.load")));
            saves[i] = addButton(new GuiButton(BUTTON_SAVE + i, right - BUTTON_WIDTH, y, BUTTON_WIDTH, 20, ""));
        }
        addButton(new GuiButton(BUTTON_BACK, this.width / 2 - 75, top + 24 + SoundPreset.MY_SLOTS * ROW_HEIGHT + 30,
                150, 20, I18n.format("gui.back")));
        updateButtons();
    }

    private void updateButtons() {
        String[] lines = WeakSpotConfig.client.sound.soundPresets;
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            int slot = i + 1;
            boolean saved = SoundPreset.mySlot(lines, slot) != null;
            loads[i].enabled = saved;
            if (slot == confirmSlot) {
                saves[i].displayString = I18n.format("weakspot.sound.preset.overwrite");
                saves[i].packedFGColour = CONFIRM_RGB;
            } else {
                saves[i].displayString = I18n.format("weakspot.sound.preset.save");
                saves[i].packedFGColour = 0;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        if (button.id >= BUTTON_SAVE && button.id < BUTTON_SAVE + SoundPreset.MY_SLOTS) {
            int slot = button.id - BUTTON_SAVE + 1;
            boolean saved = SoundPreset.mySlot(sound.soundPresets, slot) != null;
            long now = Minecraft.getSystemTime();
            if (saved && !(confirmSlot == slot && now - confirmMs < CONFIRM_MS)) {
                confirmSlot = slot;
                confirmMs = now;
            } else {
                sound.soundPresets = SoundPreset.withMySlot(sound.soundPresets, slot, SoundPreset.of(sound));
                WeakSpotConfig.save();
                confirmSlot = 0;
                done(slot, "weakspot.sound.preset.saved");
            }
            updateButtons();
            return;
        }
        confirmSlot = 0;
        if (button.id >= BUTTON_LOAD && button.id < BUTTON_LOAD + SoundPreset.MY_SLOTS) {
            int slot = button.id - BUTTON_LOAD + 1;
            SoundPreset preset = SoundPreset.mySlot(sound.soundPresets, slot);
            if (preset != null) {
                preset.applyTo(sound);
                WeakSpotConfig.save();
                SoundTab.playSwitchPreview();
                done(slot, "weakspot.sound.preset.loaded");
            }
        } else if (button.id == BUTTON_BACK) {
            mc.displayGuiScreen(parent);
            return;
        }
        updateButtons();
    }

    private void done(int slot, String key) {
        doneSlot = slot;
        doneKey = key;
        doneMs = Minecraft.getSystemTime();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        long now = Minecraft.getSystemTime();
        if (confirmSlot != 0 && now - confirmMs >= CONFIRM_MS) {
            confirmSlot = 0;
            updateButtons();
        }
        drawCenteredString(fontRenderer, I18n.format("weakspot.sound.myPresets.title"), width / 2, top, 0xFFFFFF);
        SoundTab.box(left - 4, top + 18, right + 4, top + 24 + SoundPreset.MY_SLOTS * ROW_HEIGHT);
        String[] lines = WeakSpotConfig.client.sound.soundPresets;
        int contentLeft = left + NAME_WIDTH;
        int contentWidth = right - BUTTON_WIDTH * 2 - 8 - contentLeft;
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            int slot = i + 1;
            int textY = top + 30 + i * ROW_HEIGHT;
            drawString(fontRenderer, I18n.format("weakspot.sound.preset.my", slot), left, textY, 0xFFFFFF);
            SoundPreset preset = SoundPreset.mySlot(lines, slot);
            String text;
            int color;
            if (slot == doneSlot && now - doneMs < DONE_MS) {
                text = I18n.format(doneKey);
                color = DONE_RGB;
            } else if (preset == null) {
                text = I18n.format("weakspot.sound.preset.none");
                color = SoundTab.SUBTLE_RGB;
            } else {
                text = describe(preset);
                color = 0xFFFFFF;
            }
            drawString(fontRenderer, fit(text, contentWidth), contentLeft, textY, color);
        }
        int currentY = top + 30 + SoundPreset.MY_SLOTS * ROW_HEIGHT;
        String current = I18n.format("weakspot.sound.preset.current",
                describe(SoundPreset.of(WeakSpotConfig.client.sound)));
        drawCenteredString(fontRenderer, fit(current, right - left), width / 2, currentY, SoundTab.SUBTLE_RGB);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** 中身の説明（楽器・音階の種類・動き・音域。和音がオフなら「和音オフ」も）。 */
    static String describe(SoundPreset preset) {
        String text = I18n.format("weakspot.sound.preset.describe",
                I18n.format("weakspot.sound.instrument." + preset.sound.name()),
                I18n.format("weakspot.sound.scaleType." + preset.type.name()),
                I18n.format("weakspot.sound.scaleDirection." + preset.direction.name()),
                I18n.format("weakspot.sound.preset.octaves", preset.octaves));
        return preset.chord ? text : I18n.format("weakspot.sound.preset.noChord", text);
    }

    /** 幅に入りきらなければ、末尾を「…」で切る。 */
    private String fit(String text, int width) {
        if (fontRenderer.getStringWidth(text) <= width) {
            return text;
        }
        return fontRenderer.trimStringToWidth(text, width - fontRenderer.getStringWidth("…")) + "…";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

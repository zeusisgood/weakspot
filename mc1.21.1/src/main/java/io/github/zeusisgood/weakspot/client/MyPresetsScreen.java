package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.config.ClientConfig;
import io.github.zeusisgood.weakspot.config.SoundPreset;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * 保存済みの枠の [保存] は、1 回目で「上書き？」、もう一度で上書き（3 秒かほかのボタンで戻る）。
 * 先例はバニラの保存したホットバーと、OptiFine のシェーダーのプロファイル。
 */
final class MyPresetsScreen extends Screen {

    private static final int ROW_HEIGHT = 26;
    private static final int BUTTON_WIDTH = 60;
    private static final int MAX_WIDTH = 400;
    private static final int NAME_WIDTH = 84;
    private static final int DONE_RGB = 0x55FF55;
    private static final int CONFIRM_RGB = 0xFFFF55;
    private static final long DONE_MS = 2000;
    private static final long CONFIRM_MS = 3000;

    private final Screen parent;
    private final Button[] loads = new Button[SoundPreset.MY_SLOTS];
    private final Button[] saves = new Button[SoundPreset.MY_SLOTS];
    private int left;
    private int right;
    private int top;
    /** 1 始まり。0 はなし。 */
    private int confirmSlot;
    private long confirmMs;
    private int doneSlot;
    private String doneKey;
    private long doneMs = Long.MIN_VALUE / 2;

    MyPresetsScreen(Screen parent) {
        super(Component.translatable("weakspot.sound.myPresets.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int width = Math.min(MAX_WIDTH, this.width - 20);
        left = this.width / 2 - width / 2;
        right = left + width;
        top = Math.max(20, this.height / 2 - 70);
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            int slot = i + 1;
            int y = top + 24 + i * ROW_HEIGHT;
            loads[i] = addRenderableWidget(Button.builder(Component.translatable("weakspot.sound.preset.load"),
                    b -> load(slot)).bounds(right - BUTTON_WIDTH * 2 - 4, y, BUTTON_WIDTH, 20).build());
            saves[i] = addRenderableWidget(Button.builder(Component.empty(), b -> save(slot))
                    .bounds(right - BUTTON_WIDTH, y, BUTTON_WIDTH, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(this.width / 2 - 75, top + 24 + SoundPreset.MY_SLOTS * ROW_HEIGHT + 30, 150, 20).build());
        updateButtons();
    }

    private void updateButtons() {
        String[] lines = WeakSpotConfig.client.sound.soundPresets;
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            int slot = i + 1;
            loads[i].active = SoundPreset.mySlot(lines, slot) != null;
            saves[i].setMessage(slot == confirmSlot
                    ? Component.translatable("weakspot.sound.preset.overwrite").withColor(CONFIRM_RGB)
                    : Component.translatable("weakspot.sound.preset.save"));
        }
    }

    private void save(int slot) {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        boolean saved = SoundPreset.mySlot(sound.soundPresets, slot) != null;
        long now = Util.getMillis();
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
    }

    private void load(int slot) {
        confirmSlot = 0;
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        SoundPreset preset = SoundPreset.mySlot(sound.soundPresets, slot);
        if (preset != null) {
            preset.applyTo(sound);
            WeakSpotConfig.save();
            SoundTab.playSwitchPreview();
            done(slot, "weakspot.sound.preset.loaded");
        }
        updateButtons();
    }

    private void done(int slot, String key) {
        doneSlot = slot;
        doneKey = key;
        doneMs = Util.getMillis();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        if (confirmSlot != 0 && now - confirmMs >= CONFIRM_MS) {
            confirmSlot = 0;
            updateButtons();
        }
        g.drawCenteredString(font, I18n.get("weakspot.sound.myPresets.title"), width / 2, top, 0xFFFFFFFF);
        SoundTab.box(g, left - 4, top + 18, right + 4, top + 24 + SoundPreset.MY_SLOTS * ROW_HEIGHT);
        String[] lines = WeakSpotConfig.client.sound.soundPresets;
        int contentLeft = left + NAME_WIDTH;
        int contentWidth = right - BUTTON_WIDTH * 2 - 8 - contentLeft;
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            int slot = i + 1;
            int textY = top + 30 + i * ROW_HEIGHT;
            g.drawString(font, I18n.get("weakspot.sound.preset.my", slot), left, textY, 0xFFFFFFFF);
            SoundPreset preset = SoundPreset.mySlot(lines, slot);
            String text;
            int color;
            if (slot == doneSlot && now - doneMs < DONE_MS) {
                text = I18n.get(doneKey);
                color = DONE_RGB;
            } else if (preset == null) {
                text = I18n.get("weakspot.sound.preset.none");
                color = SoundTab.SUBTLE_RGB;
            } else {
                text = describe(preset);
                color = 0xFFFFFF;
            }
            g.drawString(font, fit(text, contentWidth), contentLeft, textY, 0xFF000000 | color);
        }
        int currentY = top + 30 + SoundPreset.MY_SLOTS * ROW_HEIGHT;
        String current = I18n.get("weakspot.sound.preset.current",
                describe(SoundPreset.of(WeakSpotConfig.client.sound)));
        g.drawCenteredString(font, fit(current, right - left), width / 2, currentY, 0xFF000000 | SoundTab.SUBTLE_RGB);
    }

    /** 和音がオフなら「和音オフ」も。 */
    static String describe(SoundPreset preset) {
        String text = I18n.get("weakspot.sound.preset.describe",
                I18n.get("weakspot.sound.instrument." + preset.sound.name()),
                I18n.get("weakspot.sound.scaleType." + preset.type.name()),
                I18n.get("weakspot.sound.scaleDirection." + preset.direction.name()),
                I18n.get("weakspot.sound.preset.octaves", preset.octaves));
        return preset.chord ? text : I18n.get("weakspot.sound.preset.noChord", text);
    }

    /** 入りきらなければ、末尾を「…」で切る。 */
    private String fit(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, width - font.width("…")) + "…";
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitScale;
import io.github.zeusisgood.weakspot.common.HitSound;
import io.github.zeusisgood.weakspot.config.ClientConfig;
import io.github.zeusisgood.weakspot.config.SoundPreset;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * プリセットのボタンは OptiFine のシェーダーのプロファイルと同じ形（左クリックで次、右クリックか Shift + 左クリックで前）。
 * プリセット名は今の設定から求め（どれにも一致しなければ「カスタム」）、選んだものは保存しない。
 * 値は weakspot-client.toml にそのまま保存する（設定画面と同じ値になる）。
 */
final class SoundTab extends StatsScreenTab {

    private static final int RANDOM_WIDTH = 60;
    private static final int PREVIEW_TICKS_PER_NOTE = 4;
    /** 往復なら、上がって 1 つ下がるまで分かる数。 */
    private static final int SWITCH_PREVIEW_NOTES = 5;
    private static final int SWITCH_PREVIEW_TICKS = 2;
    static final int BOX_FILL = 0x60000000;
    static final int BOX_LINE = 0xFF555555;
    static final int SUBTLE_RGB = 0xAAAAAA;
    /** タブのボタンの列と同じ。 */
    private static final int HALF = 154;
    private static final int MY_PRESETS_WIDTH = 96;
    /** 小さい画面では下の説明を出さない。 */
    private static final int NOTE_NEEDS_HEIGHT = 217;

    private final List<AbstractWidget> buttons = new ArrayList<>();
    private Button preset;
    private Button myPresets;
    private Button random;
    private final Random rng = new Random();
    private Button mySound;
    private Button othersSound;
    private Button chord;
    private Button scaleDirection;
    private Button scaleType;
    private Button scaleOctaves;

    SoundTab(StatsScreen screen) {
        super(screen);
    }

    @Override
    void init() {
        buttons.clear();
        int left = screen.width / 2 - HALF + 4;
        int right = screen.width / 2 + HALF - 4;
        int top = screen.top();
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;

        int y = top + 49;
        preset = add(button("", left, y, right - left - MY_PRESETS_WIDTH - 4,
                b -> choosePreset(Screen.hasShiftDown() ? -1 : 1)));
        myPresets = add(button(I18n.get("weakspot.sound.myPresets"), right - MY_PRESETS_WIDTH, y, MY_PRESETS_WIDTH,
                b -> Minecraft.getInstance().setScreen(new MyPresetsScreen(screen))));

        y += 22;
        random = add(button(I18n.get("weakspot.sound.random"), right - RANDOM_WIDTH, y, RANDOM_WIDTH, b -> {
            SoundPreset.random(SoundPreset.of(sound), rng).applyTo(sound);
            WeakSpotConfig.save();
            playSwitchPreview();
            updateLabels();
        }));
        y += 22;
        mySound = add(button("", left, y, 110, b -> {
            sound.myHitSound = sound.myHitSound.next();
            onSoundChanged();
            HitSounds.playOwn(1);
        }));
        add(new VolumeSlider(left + 114, y, sound.myHitVolume, v -> {
            WeakSpotConfig.client.sound.myHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOwn(1);
        }));
        add(button(I18n.get("weakspot.sound.preview"), left + 238, y, right - left - 238, b -> {
            HitSounds.clear();
            HitSounds.playScale(HitSounds::playOwn, PREVIEW_TICKS_PER_NOTE, 0);
        }));

        int half = (right - left - 4) / 2;
        y += 22;
        scaleType = add(button("", left, y, half, b -> {
            sound.hitScaleType = sound.hitScaleType.next();
            onScaleChanged();
        }));
        scaleDirection = add(button("", right - half, y, half, b -> {
            sound.hitScaleDirection = sound.hitScaleDirection.next();
            onScaleChanged();
        }));
        y += 22;
        scaleOctaves = add(button("", left, y, half, b -> {
            sound.hitScaleOctaves = sound.hitScaleOctaves >= HitScale.MAX_OCTAVES ? HitScale.MIN_OCTAVES
                    : sound.hitScaleOctaves + 1;
            onScaleChanged();
        }));
        chord = add(button("", right - half, y, half, b -> {
            sound.hitChordEnabled = !sound.hitChordEnabled;
            onSoundChanged();
        }));

        y = top + 178;
        othersSound = add(button("", left, y, 110, b -> {
            sound.othersHitSound = sound.othersHitSound.next();
            WeakSpotConfig.save();
            updateLabels();
            HitSounds.playOtherFlat(1);
        }));
        add(new VolumeSlider(left + 114, y, sound.othersHitVolume, v -> {
            WeakSpotConfig.client.sound.othersHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOtherFlat(1);
        }));
        add(button(I18n.get("weakspot.sound.preview"), left + 238, y, right - left - 238, b -> {
            HitSounds.clear();
            HitSounds.playScale(HitSounds::playOtherFlat, PREVIEW_TICKS_PER_NOTE, 0);
        }));

        updateLabels();
    }

    private static Button button(String text, int x, int y, int width, Button.OnPress onPress) {
        return Button.builder(Component.literal(text), onPress).bounds(x, y, width, 20).build();
    }

    private <T extends AbstractWidget> T add(T button) {
        buttons.add(button);
        return screen.add(button);
    }

    @Override
    void show(boolean shown) {
        for (AbstractWidget button : buttons) {
            button.visible = shown;
        }
    }

    /** バニラのボタンは右クリックを拾わないので、ここで受ける。 */
    @Override
    void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 1 && preset.visible && preset.active && preset.isMouseOver(mouseX, mouseY)) {
            preset.playDownSound(Minecraft.getInstance().getSoundManager());
            choosePreset(-1);
        }
    }

    /** 最初から入っているもの → 保存してあるマイプリセット（空きは飛ばす）。 */
    private static List<SoundPreset> cycle() {
        List<SoundPreset> list = new ArrayList<>(SoundPreset.BUILT_IN);
        for (int slot = 1; slot <= SoundPreset.MY_SLOTS; slot++) {
            SoundPreset p = SoundPreset.mySlot(WeakSpotConfig.client.sound.soundPresets, slot);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    /** 最初から入っているものを先に探す。なければ -1（カスタム）。 */
    private static int matchIndex(List<SoundPreset> cycle) {
        SoundPreset current = SoundPreset.of(WeakSpotConfig.client.sound);
        for (int i = 0; i < cycle.size(); i++) {
            if (current.sameSound(cycle.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** step は 1 か -1。カスタムからは、次なら最初、前なら最後へ。 */
    private void choosePreset(int step) {
        List<SoundPreset> cycle = cycle();
        int index = matchIndex(cycle);
        int next = index < 0 ? (step > 0 ? 0 : cycle.size() - 1) : Math.floorMod(index + step, cycle.size());
        cycle.get(next).applyTo(WeakSpotConfig.client.sound);
        WeakSpotConfig.save();
        playSwitchPreview();
        updateLabels();
    }

    private void onSoundChanged() {
        WeakSpotConfig.save();
        updateLabels();
    }

    /** 続けて押したら前の試聴を止める。 */
    private void onScaleChanged() {
        onSoundChanged();
        playSwitchPreview();
    }

    /** 往復は、上がりきる手前から鳴らして、折り返しが分かるようにする。 */
    static void playSwitchPreview() {
        HitSounds.clear();
        HitScale scale = HitSounds.scale();
        int start = scale.direction == HitScale.Direction.UP_DOWN
                ? Math.max(0, scale.melodyLength() - SWITCH_PREVIEW_NOTES + 1) : 0;
        HitSounds.playScale(streak -> HitSounds.playOwn(streak + start), SWITCH_PREVIEW_TICKS, 0,
                SWITCH_PREVIEW_NOTES);
    }

    private void updateLabels() {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        preset.setMessage(Component.literal(I18n.get("weakspot.sound.preset", currentPresetName())));
        mySound.setMessage(Component.literal(instrumentLabel(sound.myHitSound)));
        othersSound.setMessage(Component.literal(instrumentLabel(sound.othersHitSound)));
        chord.setMessage(Component.literal(I18n.get("weakspot.sound.chordShort",
                I18n.get(sound.hitChordEnabled ? "options.on" : "options.off"))));
        scaleDirection.setMessage(Component.literal(I18n.get("weakspot.sound.scaleDirection",
                I18n.get("weakspot.sound.scaleDirection." + sound.hitScaleDirection.name()))));
        scaleType.setMessage(Component.literal(I18n.get("weakspot.sound.scaleType",
                I18n.get("weakspot.sound.scaleType." + sound.hitScaleType.name()))));
        scaleOctaves.setMessage(Component.literal(I18n.get("weakspot.sound.scaleOctaves", sound.hitScaleOctaves)));
    }

    private static String currentPresetName() {
        SoundPreset current = SoundPreset.of(WeakSpotConfig.client.sound);
        for (SoundPreset p : SoundPreset.BUILT_IN) {
            if (current.sameSound(p)) {
                return I18n.get("weakspot.sound.preset." + p.key);
            }
        }
        for (int slot = 1; slot <= SoundPreset.MY_SLOTS; slot++) {
            if (current.sameSound(SoundPreset.mySlot(WeakSpotConfig.client.sound.soundPresets, slot))) {
                return I18n.get("weakspot.sound.preset.my", slot);
            }
        }
        return I18n.get("weakspot.sound.preset.custom");
    }

    private static String instrumentLabel(HitSound sound) {
        return I18n.get("weakspot.sound.instrument", I18n.get("weakspot.sound.instrument." + sound.name()));
    }

    @Override
    void draw(GuiGraphics g) {
        int center = screen.width / 2;
        int left = center - HALF + 4;
        int right = center + HALF - 4;
        int top = screen.top();
        // ボタンより先に描くので、ボタンの後ろになる
        box(g, center - HALF, top + 37, center + HALF, top + 163);
        box(g, center - HALF, top + 167, center + HALF, top + 202);
        screen.drawString(g, I18n.get("weakspot.sound.mine"), left, top + 39, 0xFFFFFF);
        screen.drawString(g, I18n.get("weakspot.sound.others"), left, top + 169, 0xFFFFFF);
        String fine = I18n.get("weakspot.sound.fineSettings");
        int textY = top + 77;
        screen.drawString(g, fine, left, textY, SUBTLE_RGB);
        int lineX = left + screen.font().width(fine) + 4;
        g.fill(lineX, textY + 4, right - RANDOM_WIDTH - 4, textY + 5, BOX_LINE);

        if (screen.bottom() - top >= NOTE_NEEDS_HEIGHT) {
            screen.drawCenteredString(g, I18n.get("weakspot.sound.note"), center, top + 206, SUBTLE_RGB);
        }
        int mx = screen.mouseX();
        int my = screen.mouseY();
        if (over(preset, mx, my)) {
            screen.setTooltip(I18n.get("weakspot.sound.preset.tooltip"));
        } else if (over(random, mx, my)) {
            screen.setTooltip(I18n.get("weakspot.sound.random.tooltip"));
        } else if (over(myPresets, mx, my)) {
            screen.setTooltip(I18n.get("weakspot.sound.myPresets.tooltip"));
        } else if (over(chord, mx, my)) {
            screen.setTooltip(I18n.get("weakspot.sound.comboNote"));
        }
    }

    private static boolean over(AbstractWidget button, int mx, int my) {
        return button.visible && mx >= button.getX() && mx < button.getX() + button.getWidth() && my >= button.getY()
                && my < button.getY() + button.getHeight();
    }

    static void box(GuiGraphics g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, BOX_FILL);
        g.fill(left, top, right, top + 1, BOX_LINE);
        g.fill(left, bottom - 1, right, bottom, BOX_LINE);
        g.fill(left, top, left + 1, bottom, BOX_LINE);
        g.fill(right - 1, top, right, bottom, BOX_LINE);
    }

    /** 動かし終えたとき（マウスを離したとき）に、値を保存して 1 音鳴らす。値は 0〜1、1% 刻み。 */
    private static final class VolumeSlider extends AbstractSliderButton {

        private final DoubleConsumer onRelease;
        /** 押していないボタンの上で離したときにも onRelease が呼ばれるので、押したかを覚える。 */
        private boolean pressed;

        VolumeSlider(int x, int y, double volume, DoubleConsumer onRelease) {
            super(x, y, 120, 20, Component.empty(), Math.round(volume * 100) / 100.0);
            this.onRelease = onRelease;
            updateMessage();
        }

        private int percent() {
            return (int) Math.round(value * 100);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(I18n.get("weakspot.sound.volume") + " " + percent() + "%"));
        }

        @Override
        protected void applyValue() {
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            pressed = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            if (pressed) {
                pressed = false;
                onRelease.accept(percent() / 100.0);
            }
        }

        /** 矢印キーで動かしたときは、1 段ごとに保存する。 */
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            double before = value;
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (value != before) {
                onRelease.accept(percent() / 100.0);
            }
            return handled;
        }
    }
}

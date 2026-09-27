package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.config.HitSound;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * 統計画面の「サウンド」タブ（1.8.9 で StatsScreen から分けた）。ヒット音の楽器と音量を変えて試聴する。
 * クライアントだけで完結し、値は weakspot.cfg にそのまま保存する（Forge の設定画面と同じ値になる）。
 */
final class SoundTab extends StatsScreenTab {

    private static final int BUTTON_MY_SOUND = 20;
    private static final int BUTTON_MY_VOLUME = 21;
    private static final int BUTTON_MY_PREVIEW = 22;
    private static final int BUTTON_OTHERS_SOUND = 30;
    private static final int BUTTON_OTHERS_VOLUME = 31;
    private static final int BUTTON_OTHERS_PREVIEW = 32;
    /** 自分のヒット音に、コンボで音を重ねるか（1.9.5）。 */
    private static final int BUTTON_CHORD = 40;
    /** 試聴で音階を鳴らす間隔（tick）。 */
    private static final int PREVIEW_TICKS_PER_NOTE = 4;

    private final List<GuiButton> buttons = new ArrayList<>();
    private GuiButton mySound;
    private GuiButton othersSound;
    private GuiButton chord;

    SoundTab(StatsScreen screen) {
        super(screen);
    }

    @Override
    void init() {
        buttons.clear();
        int center = screen.width / 2;
        int y = screen.top() + 56;
        mySound = add(new GuiButton(BUTTON_MY_SOUND, center - 154, y, 120, 20, ""));
        add(new VolumeSlider(BUTTON_MY_VOLUME, center - 30, y, WeakSpotConfig.client.sound.myHitVolume, v -> {
            WeakSpotConfig.client.sound.myHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOwn(1);
        }));
        add(new GuiButton(BUTTON_MY_PREVIEW, center + 94, y, 60, 20, I18n.format("weakspot.sound.preview")));

        y += 48;
        othersSound = add(new GuiButton(BUTTON_OTHERS_SOUND, center - 154, y, 120, 20, ""));
        add(new VolumeSlider(BUTTON_OTHERS_VOLUME, center - 30, y, WeakSpotConfig.client.sound.othersHitVolume, v -> {
            WeakSpotConfig.client.sound.othersHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOtherFlat(1);
        }));
        add(new GuiButton(BUTTON_OTHERS_PREVIEW, center + 94, y, 60, 20, I18n.format("weakspot.sound.preview")));

        chord = add(new GuiButton(BUTTON_CHORD, center - 100, screen.top() + 146, 200, 20, ""));
        updateLabels();
    }

    private <T extends GuiButton> T add(T button) {
        buttons.add(button);
        return screen.add(button);
    }

    @Override
    void show(boolean shown) {
        for (GuiButton button : buttons) {
            button.visible = shown;
        }
    }

    @Override
    boolean action(int id) {
        switch (id) {
            case BUTTON_MY_SOUND:
                WeakSpotConfig.client.sound.myHitSound = WeakSpotConfig.client.sound.myHitSound.next();
                WeakSpotConfig.save();
                updateLabels();
                HitSounds.playOwn(1);
                return true;
            case BUTTON_OTHERS_SOUND:
                WeakSpotConfig.client.sound.othersHitSound = WeakSpotConfig.client.sound.othersHitSound.next();
                WeakSpotConfig.save();
                updateLabels();
                HitSounds.playOtherFlat(1);
                return true;
            case BUTTON_CHORD:
                WeakSpotConfig.client.sound.hitChordEnabled = !WeakSpotConfig.client.sound.hitChordEnabled;
                WeakSpotConfig.save();
                updateLabels();
                return true;
            case BUTTON_MY_PREVIEW:
                HitSounds.clear();
                HitSounds.playScale(HitSounds::playOwn, PREVIEW_TICKS_PER_NOTE, 0);
                return true;
            case BUTTON_OTHERS_PREVIEW:
                HitSounds.clear();
                HitSounds.playScale(HitSounds::playOtherFlat, PREVIEW_TICKS_PER_NOTE, 0);
                return true;
            default:
                return false;
        }
    }

    private void updateLabels() {
        mySound.displayString = instrumentLabel(WeakSpotConfig.client.sound.myHitSound);
        othersSound.displayString = instrumentLabel(WeakSpotConfig.client.sound.othersHitSound);
        chord.displayString = I18n.format("weakspot.sound.chord",
                I18n.format(WeakSpotConfig.client.sound.hitChordEnabled ? "options.on" : "options.off"));
    }

    private static String instrumentLabel(HitSound sound) {
        return I18n.format("weakspot.sound.instrument", I18n.format("weakspot.sound.instrument." + sound.name()));
    }

    @Override
    void draw() {
        int labelX = screen.width / 2 - 154;
        int top = screen.top();
        screen.drawString(screen.font(), I18n.format("weakspot.sound.mine"), labelX, top + 44, 0xFFFFFF);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.others"), labelX, top + 92, 0xFFFFFF);
        screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.note"), screen.width / 2, top + 132,
                0xAAAAAA);
        screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.comboNote"), screen.width / 2,
                top + 172, 0xAAAAAA);
    }

    /** 音量（0〜100%）のスライダー。動かし終えたとき（マウスを離したとき）に、値を保存して1音鳴らす。 */
    private static final class VolumeSlider extends GuiSlider {

        private final DoubleConsumer onRelease;

        VolumeSlider(int id, int x, int y, double volume, DoubleConsumer onRelease) {
            super(id, x, y, 120, 20, I18n.format("weakspot.sound.volume") + " ", "%", 0, 100,
                    Math.round(volume * 100), false, true);
            this.onRelease = onRelease;
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY) {
            boolean wasDragging = dragging;
            super.mouseReleased(mouseX, mouseY);
            if (wasDragging) {
                onRelease.accept(getValueInt() / 100.0);
            }
        }
    }
}

package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitScale;
import io.github.zeusisgood.weakspot.config.HitSound;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * 統計画面の「サウンド」タブ（1.8.9 で StatsScreen から分けた）。ヒット音の楽器と音量、音階（1.11.1）を変えて試聴する。
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
    /** 音階の動き・種類・音域（1.11.1）。 */
    private static final int BUTTON_SCALE_DIRECTION = 41;
    private static final int BUTTON_SCALE_TYPE = 42;
    private static final int BUTTON_SCALE_OCTAVES = 43;
    /** 音階の設定を切り替えたときの短い試聴（音の数と間隔 tick）。往復なら上がって 1 つ下がるまで分かる数。 */
    private static final int SWITCH_PREVIEW_NOTES = 5;
    private static final int SWITCH_PREVIEW_TICKS = 2;
    /** 試聴で音階を鳴らす間隔（tick）。 */
    private static final int PREVIEW_TICKS_PER_NOTE = 4;

    private final List<GuiButton> buttons = new ArrayList<>();
    private GuiButton mySound;
    private GuiButton othersSound;
    private GuiButton chord;
    private GuiButton scaleDirection;
    private GuiButton scaleType;
    private GuiButton scaleOctaves;

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

        int rows = screen.top() + 140;
        chord = add(new GuiButton(BUTTON_CHORD, center - 154, rows, 150, 20, ""));
        scaleDirection = add(new GuiButton(BUTTON_SCALE_DIRECTION, center + 4, rows, 150, 20, ""));
        scaleType = add(new GuiButton(BUTTON_SCALE_TYPE, center - 154, rows + 24, 150, 20, ""));
        scaleOctaves = add(new GuiButton(BUTTON_SCALE_OCTAVES, center + 4, rows + 24, 150, 20, ""));
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
            case BUTTON_SCALE_DIRECTION:
                WeakSpotConfig.client.sound.hitScaleDirection = WeakSpotConfig.client.sound.hitScaleDirection.next();
                onScaleChanged();
                return true;
            case BUTTON_SCALE_TYPE:
                WeakSpotConfig.client.sound.hitScaleType = WeakSpotConfig.client.sound.hitScaleType.next();
                onScaleChanged();
                return true;
            case BUTTON_SCALE_OCTAVES:
                WeakSpotConfig.client.sound.hitScaleOctaves = WeakSpotConfig.client.sound.hitScaleOctaves
                        >= HitScale.MAX_OCTAVES ? HitScale.MIN_OCTAVES : WeakSpotConfig.client.sound.hitScaleOctaves + 1;
                onScaleChanged();
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

    /**
     * 音階の設定を切り替えた: 保存して、新しい設定の始めの数音を速めに鳴らす（1.11.1。続けて押したら前の試聴を止める）。
     * 往復は、上がりきる手前から鳴らして、折り返しが分かるようにする。
     */
    private void onScaleChanged() {
        WeakSpotConfig.save();
        updateLabels();
        HitSounds.clear();
        HitScale scale = HitSounds.scale();
        int start = scale.direction == HitScale.Direction.UP_DOWN
                ? Math.max(0, scale.melodyLength() - SWITCH_PREVIEW_NOTES + 1) : 0;
        HitSounds.playScale(streak -> HitSounds.playOwn(streak + start), SWITCH_PREVIEW_TICKS, 0,
                SWITCH_PREVIEW_NOTES);
    }

    private void updateLabels() {
        mySound.displayString = instrumentLabel(WeakSpotConfig.client.sound.myHitSound);
        othersSound.displayString = instrumentLabel(WeakSpotConfig.client.sound.othersHitSound);
        chord.displayString = I18n.format("weakspot.sound.chord",
                I18n.format(WeakSpotConfig.client.sound.hitChordEnabled ? "options.on" : "options.off"));
        scaleDirection.displayString = I18n.format("weakspot.sound.scaleDirection",
                I18n.format("weakspot.sound.scaleDirection." + WeakSpotConfig.client.sound.hitScaleDirection.name()));
        scaleType.displayString = I18n.format("weakspot.sound.scaleType",
                I18n.format("weakspot.sound.scaleType." + WeakSpotConfig.client.sound.hitScaleType.name()));
        scaleOctaves.displayString = I18n.format("weakspot.sound.scaleOctaves",
                WeakSpotConfig.client.sound.hitScaleOctaves);
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
        screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.note"), screen.width / 2, top + 128,
                0xAAAAAA);
        screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.comboNote"), screen.width / 2,
                top + 190, 0xAAAAAA);
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

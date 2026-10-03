package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitScale;
import io.github.zeusisgood.weakspot.config.ClientConfig;
import io.github.zeusisgood.weakspot.config.HitSound;
import io.github.zeusisgood.weakspot.config.SoundPreset;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * 統計画面の「サウンド」タブ（1.8.9 で StatsScreen から分けた）。ヒット音の楽器と音量、音階（1.11.1）を変えて試聴する。
 * 1.11.1 から、自分の音とほかの人の音を枠で分け、音のプリセット（最初から入っている 6 つと、マイプリセット 1〜3。
 * SoundPreset）を ◀ ▶ で選べる。プリセット名の表示は今の設定から求め（どれにも一致しなければ「カスタム」）、選んだもの
 * は保存しない。クライアントだけで完結し、値は weakspot.cfg にそのまま保存する（Forge の設定画面と同じ値になる）。
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
    /** プリセット（1.11.1）: 前・次・今の設定を保存。保存先を選ぶ行では、前・次が保存先を選び、保存・やめる。 */
    private static final int BUTTON_PRESET_PREV = 50;
    private static final int BUTTON_PRESET_NEXT = 51;
    private static final int BUTTON_SAVE_CURRENT = 52;
    private static final int BUTTON_SAVE = 53;
    private static final int BUTTON_SAVE_CANCEL = 54;
    /** 試聴で音階を鳴らす間隔（tick）。 */
    private static final int PREVIEW_TICKS_PER_NOTE = 4;
    /** 音階の設定を切り替えたときの短い試聴（音の数と間隔 tick）。往復なら上がって 1 つ下がるまで分かる数。 */
    private static final int SWITCH_PREVIEW_NOTES = 5;
    private static final int SWITCH_PREVIEW_TICKS = 2;
    /** 枠（半透明の黒 #000000 と、枠線の灰 #555555）。 */
    private static final int BOX_FILL = 0x60000000;
    private static final int BOX_LINE = 0xFF555555;
    /** 中身の幅の半分（タブのボタンの列と同じ）。 */
    private static final int HALF = 154;

    private final List<GuiButton> buttons = new ArrayList<>();
    /** プリセットを選ぶ行と、保存先を選ぶ行のボタン（どちらか一方だけを出す）。 */
    private final List<GuiButton> presetRow = new ArrayList<>();
    private final List<GuiButton> saveRow = new ArrayList<>();
    private GuiButton mySound;
    private GuiButton othersSound;
    private GuiButton chord;
    private GuiButton scaleDirection;
    private GuiButton scaleType;
    private GuiButton scaleOctaves;
    private GuiButton save;

    private boolean shown;
    /** 保存先を選んでいる間か、と、その保存先（1 始まり）。 */
    private boolean saving;
    private int saveSlot = 1;
    /**
     * プリセットの表示の位置（0〜5 が最初から入っているもの、6〜8 がマイプリセット、-1 はカスタム）。設定を変えたら
     * 今の設定から求め直す。空きのマイプリセットを選んだときだけ、設定と一致しない位置を指す。
     */
    private int shownIndex = -1;

    SoundTab(StatsScreen screen) {
        super(screen);
    }

    @Override
    void init() {
        buttons.clear();
        presetRow.clear();
        saveRow.clear();
        int left = screen.width / 2 - HALF + 4;
        int right = screen.width / 2 + HALF - 4;
        int top = screen.top();

        // 自分のヒット音
        int y = top + 50;
        presetRow.add(add(new GuiButton(BUTTON_PRESET_PREV, left, y, 20, 20, "◀")));
        presetRow.add(add(new GuiButton(BUTTON_PRESET_NEXT, left + 160, y, 20, 20, "▶")));
        presetRow.add(add(new GuiButton(BUTTON_SAVE_CURRENT, left + 184, y, right - left - 184, 20,
                I18n.format("weakspot.sound.preset.saveCurrent"))));
        saveRow.add(add(new GuiButton(BUTTON_PRESET_PREV + 100, left, y, 20, 20, "◀")));
        saveRow.add(add(new GuiButton(BUTTON_PRESET_NEXT + 100, left + 160, y, 20, 20, "▶")));
        save = add(new GuiButton(BUTTON_SAVE, left + 184, y, 70, 20, ""));
        saveRow.add(save);
        saveRow.add(add(new GuiButton(BUTTON_SAVE_CANCEL, left + 258, y, right - left - 258, 20,
                I18n.format("weakspot.sound.preset.cancel"))));

        y += 22;
        mySound = add(new GuiButton(BUTTON_MY_SOUND, left, y, 110, 20, ""));
        add(new VolumeSlider(BUTTON_MY_VOLUME, left + 114, y, WeakSpotConfig.client.sound.myHitVolume, v -> {
            WeakSpotConfig.client.sound.myHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOwn(1);
        }));
        add(new GuiButton(BUTTON_MY_PREVIEW, left + 238, y, right - left - 238, 20,
                I18n.format("weakspot.sound.preview")));

        int half = (right - left - 4) / 2;
        y += 22;
        scaleType = add(new GuiButton(BUTTON_SCALE_TYPE, left, y, half, 20, ""));
        scaleDirection = add(new GuiButton(BUTTON_SCALE_DIRECTION, right - half, y, half, 20, ""));
        y += 22;
        scaleOctaves = add(new GuiButton(BUTTON_SCALE_OCTAVES, left, y, half, 20, ""));
        chord = add(new GuiButton(BUTTON_CHORD, right - half, y, half, 20, ""));

        // ほかの人のヒット音
        y = top + 156;
        othersSound = add(new GuiButton(BUTTON_OTHERS_SOUND, left, y, 110, 20, ""));
        add(new VolumeSlider(BUTTON_OTHERS_VOLUME, left + 114, y, WeakSpotConfig.client.sound.othersHitVolume, v -> {
            WeakSpotConfig.client.sound.othersHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOtherFlat(1);
        }));
        add(new GuiButton(BUTTON_OTHERS_PREVIEW, left + 238, y, right - left - 238, 20,
                I18n.format("weakspot.sound.preview")));

        saving = false;
        shownIndex = matchIndex();
        updateLabels();
    }

    private <T extends GuiButton> T add(T button) {
        buttons.add(button);
        return screen.add(button);
    }

    @Override
    void show(boolean shown) {
        this.shown = shown;
        for (GuiButton button : buttons) {
            button.visible = shown;
        }
        updateRows();
    }

    /** プリセットを選ぶ行と、保存先を選ぶ行の、どちらかだけを出す。 */
    private void updateRows() {
        for (GuiButton button : presetRow) {
            button.visible = shown && !saving;
        }
        for (GuiButton button : saveRow) {
            button.visible = shown && saving;
        }
    }

    @Override
    boolean action(int id) {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        switch (id) {
            case BUTTON_MY_SOUND:
                sound.myHitSound = sound.myHitSound.next();
                onSoundChanged();
                HitSounds.playOwn(1);
                return true;
            case BUTTON_OTHERS_SOUND:
                sound.othersHitSound = sound.othersHitSound.next();
                WeakSpotConfig.save();
                updateLabels();
                HitSounds.playOtherFlat(1);
                return true;
            case BUTTON_CHORD:
                sound.hitChordEnabled = !sound.hitChordEnabled;
                onSoundChanged();
                return true;
            case BUTTON_SCALE_DIRECTION:
                sound.hitScaleDirection = sound.hitScaleDirection.next();
                onScaleChanged();
                return true;
            case BUTTON_SCALE_TYPE:
                sound.hitScaleType = sound.hitScaleType.next();
                onScaleChanged();
                return true;
            case BUTTON_SCALE_OCTAVES:
                sound.hitScaleOctaves = sound.hitScaleOctaves >= HitScale.MAX_OCTAVES ? HitScale.MIN_OCTAVES
                        : sound.hitScaleOctaves + 1;
                onScaleChanged();
                return true;
            case BUTTON_PRESET_PREV:
            case BUTTON_PRESET_NEXT:
                choosePreset(id == BUTTON_PRESET_NEXT ? 1 : -1);
                return true;
            case BUTTON_SAVE_CURRENT:
                saving = true;
                saveSlot = shownIndex >= SoundPreset.BUILT_IN.size() ? shownIndex - SoundPreset.BUILT_IN.size() + 1 : 1;
                updateRows();
                updateLabels();
                return true;
            case BUTTON_PRESET_PREV + 100:
            case BUTTON_PRESET_NEXT + 100:
                saveSlot = Math.floorMod(saveSlot - 1 + (id == BUTTON_PRESET_NEXT + 100 ? 1 : -1),
                        SoundPreset.MY_SLOTS) + 1;
                updateLabels();
                return true;
            case BUTTON_SAVE:
                sound.soundPresets = SoundPreset.withMySlot(sound.soundPresets, saveSlot, SoundPreset.of(sound));
                WeakSpotConfig.save();
                saving = false;
                shownIndex = SoundPreset.BUILT_IN.size() + saveSlot - 1;
                updateRows();
                updateLabels();
                return true;
            case BUTTON_SAVE_CANCEL:
                saving = false;
                updateRows();
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

    /** プリセットの数（最初から入っているものとマイプリセット）。 */
    private static int presetCount() {
        return SoundPreset.BUILT_IN.size() + SoundPreset.MY_SLOTS;
    }

    /** 位置 index のプリセット（空きのマイプリセットは null）。 */
    private static SoundPreset presetAt(int index) {
        if (index < SoundPreset.BUILT_IN.size()) {
            return SoundPreset.BUILT_IN.get(index);
        }
        return SoundPreset.mySlot(WeakSpotConfig.client.sound.soundPresets, index - SoundPreset.BUILT_IN.size() + 1);
    }

    /** 今の設定と一致するプリセットの位置（最初から入っているものを先に探す。なければ -1 = カスタム）。 */
    private static int matchIndex() {
        SoundPreset current = SoundPreset.of(WeakSpotConfig.client.sound);
        for (int i = 0; i < presetCount(); i++) {
            if (current.sameSound(presetAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /** ◀ ▶ で次のプリセットへ。空きのマイプリセットなら設定を変えない。それ以外は反映して保存し、短く鳴らす。 */
    private void choosePreset(int step) {
        int count = presetCount();
        shownIndex = shownIndex < 0 ? (step > 0 ? 0 : count - 1) : Math.floorMod(shownIndex + step, count);
        SoundPreset preset = presetAt(shownIndex);
        if (preset != null) {
            preset.applyTo(WeakSpotConfig.client.sound);
            WeakSpotConfig.save();
            playSwitchPreview();
        }
        updateLabels();
    }

    /** 楽器・和音を変えた: 保存して、プリセットの表示を今の設定から求め直す。 */
    private void onSoundChanged() {
        WeakSpotConfig.save();
        shownIndex = matchIndex();
        updateLabels();
    }

    /** 音階の設定を切り替えた: 保存して、新しい設定の始めの数音を速めに鳴らす（1.11.1。続けて押したら前の試聴を止める）。 */
    private void onScaleChanged() {
        onSoundChanged();
        playSwitchPreview();
    }

    /** 今の設定の始めの数音を速めに鳴らす。往復は、上がりきる手前から鳴らして、折り返しが分かるようにする。 */
    private static void playSwitchPreview() {
        HitSounds.clear();
        HitScale scale = HitSounds.scale();
        int start = scale.direction == HitScale.Direction.UP_DOWN
                ? Math.max(0, scale.melodyLength() - SWITCH_PREVIEW_NOTES + 1) : 0;
        HitSounds.playScale(streak -> HitSounds.playOwn(streak + start), SWITCH_PREVIEW_TICKS, 0,
                SWITCH_PREVIEW_NOTES);
    }

    private void updateLabels() {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        mySound.displayString = instrumentLabel(sound.myHitSound);
        othersSound.displayString = instrumentLabel(sound.othersHitSound);
        chord.displayString = I18n.format("weakspot.sound.chordShort",
                I18n.format(sound.hitChordEnabled ? "options.on" : "options.off"));
        scaleDirection.displayString = I18n.format("weakspot.sound.scaleDirection",
                I18n.format("weakspot.sound.scaleDirection." + sound.hitScaleDirection.name()));
        scaleType.displayString = I18n.format("weakspot.sound.scaleType",
                I18n.format("weakspot.sound.scaleType." + sound.hitScaleType.name()));
        scaleOctaves.displayString = I18n.format("weakspot.sound.scaleOctaves", sound.hitScaleOctaves);
        save.displayString = I18n.format(SoundPreset.mySlot(sound.soundPresets, saveSlot) == null
                ? "weakspot.sound.preset.save" : "weakspot.sound.preset.overwrite");
    }

    /** プリセットの位置の名前（空きのマイプリセットは「（空き）」付き、-1 はカスタム）。 */
    private static String presetName(int index) {
        if (index < 0) {
            return I18n.format("weakspot.sound.preset.custom");
        }
        if (index < SoundPreset.BUILT_IN.size()) {
            return I18n.format("weakspot.sound.preset." + SoundPreset.BUILT_IN.get(index).key);
        }
        return mySlotName(index - SoundPreset.BUILT_IN.size() + 1);
    }

    private static String mySlotName(int slot) {
        String name = I18n.format("weakspot.sound.preset.my", slot);
        return SoundPreset.mySlot(WeakSpotConfig.client.sound.soundPresets, slot) == null
                ? I18n.format("weakspot.sound.preset.empty", name) : name;
    }

    private static String instrumentLabel(HitSound sound) {
        return I18n.format("weakspot.sound.instrument", I18n.format("weakspot.sound.instrument." + sound.name()));
    }

    @Override
    void draw() {
        int center = screen.width / 2;
        int left = center - HALF + 4;
        int top = screen.top();
        // 自分の音とほかの人の音の枠（ボタンより先に描くので、ボタンの後ろになる）
        box(center - HALF, top + 37, center + HALF, top + 139);
        box(center - HALF, top + 143, center + HALF, top + 179);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.mine"), left, top + 40, 0xFFFFFF);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.others"), left, top + 146, 0xFFFFFF);
        // プリセットの名前（◀ と ▶ の間）。保存先を選んでいる間は保存先の枠の名前（黄）
        String label = saving ? mySlotName(saveSlot)
                : I18n.format("weakspot.sound.preset", presetName(shownIndex));
        int nameCenter = left + 90;
        int width = screen.font().getStringWidth(label);
        if (width > 136) {
            label = screen.font().trimStringToWidth(label, 130) + "…";
            width = screen.font().getStringWidth(label);
        }
        screen.drawString(screen.font(), label, nameCenter - width / 2, top + 56, saving ? 0xFFFF55 : 0xFFFFFF);
        screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.note"), center, top + 183, 0xAAAAAA);
        screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.comboNote"), center, top + 194,
                0xAAAAAA);
    }

    /** 薄い枠（半透明の黒と、灰の枠線）。 */
    private static void box(int left, int top, int right, int bottom) {
        Gui.drawRect(left, top, right, bottom, BOX_FILL);
        Gui.drawRect(left, top, right, top + 1, BOX_LINE);
        Gui.drawRect(left, bottom - 1, right, bottom, BOX_LINE);
        Gui.drawRect(left, top, left + 1, bottom, BOX_LINE);
        Gui.drawRect(right - 1, top, right, bottom, BOX_LINE);
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

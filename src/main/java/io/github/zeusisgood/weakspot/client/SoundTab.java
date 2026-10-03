package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitScale;
import io.github.zeusisgood.weakspot.config.ClientConfig;
import io.github.zeusisgood.weakspot.config.HitSound;
import io.github.zeusisgood.weakspot.config.SoundPreset;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * 統計画面の「サウンド」タブ（1.8.9 で StatsScreen から分けた）。ヒット音の楽器と音量、音階（1.11.1）を変えて試聴する。
 * 1.11.1 から、自分の音とほかの人の音を枠で分け、音のプリセット（最初から入っている 6 つと、マイプリセット 1〜3。
 * SoundPreset）を ◀ ▶ で選べる。プリセット名の表示は今の設定から求め（どれにも一致しなければ「カスタム」）、選んだもの
 * は保存しない。マイプリセットへの保存は専用の行の [1] [2] [3]（保存済みの枠は「上書き？」のあと、もう一度押すと上書き）。
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
    /** プリセット（1.11.1）: 前・次と、マイプリセットの枠 1〜3 に保存（BUTTON_SAVE_SLOT + 0〜2）。 */
    private static final int BUTTON_PRESET_PREV = 50;
    private static final int BUTTON_PRESET_NEXT = 51;
    private static final int BUTTON_SAVE_SLOT = 60;
    /** 試聴で音階を鳴らす間隔（tick）。 */
    private static final int PREVIEW_TICKS_PER_NOTE = 4;
    /** 音階の設定を切り替えたときの短い試聴（音の数と間隔 tick）。往復なら上がって 1 つ下がるまで分かる数。 */
    private static final int SWITCH_PREVIEW_NOTES = 5;
    private static final int SWITCH_PREVIEW_TICKS = 2;
    /** 枠（半透明の黒 #000000 と、枠線の灰 #555555）。 */
    private static final int BOX_FILL = 0x60000000;
    private static final int BOX_LINE = 0xFF555555;
    /** 保存しました（緑 #55FF55）・上書き？（黄 #FFFF55）・空き（灰 #AAAAAA）。 */
    private static final int SAVED_RGB = 0x55FF55;
    private static final int CONFIRM_RGB = 0xFFFF55;
    private static final int EMPTY_RGB = 0xAAAAAA;
    /** 「保存しました」を出す長さと、「上書き？」が元に戻るまでの長さ（ミリ秒）。 */
    private static final long SAVED_MS = 2000;
    private static final long CONFIRM_MS = 3000;
    /** 中身の幅の半分（タブのボタンの列と同じ）。 */
    private static final int HALF = 154;
    /** 下の説明を出すのに要る、上から下のボタンまでの高さ（小さい画面では出さない）。 */
    private static final int NOTE_NEEDS_HEIGHT = 212;

    private final List<GuiButton> buttons = new ArrayList<>();
    private final GuiButton[] saveSlots = new GuiButton[SoundPreset.MY_SLOTS];
    private GuiButton mySound;
    private GuiButton othersSound;
    private GuiButton chord;
    private GuiButton scaleDirection;
    private GuiButton scaleType;
    private GuiButton scaleOctaves;

    /**
     * プリセットの表示の位置（0〜5 が最初から入っているもの、6〜8 がマイプリセット、-1 はカスタム）。設定を変えたら
     * 今の設定から求め直す。空きのマイプリセットを選んだときだけ、設定と一致しない位置を指す。
     */
    private int shownIndex = -1;
    /** 上書きの確かめ中の枠（1 始まり。0 はなし）と、その時刻。 */
    private int confirmSlot;
    private long confirmMs;
    /** 「保存しました」を出している枠と、その時刻。 */
    private int savedSlot;
    private long savedMs = Long.MIN_VALUE / 2;

    SoundTab(StatsScreen screen) {
        super(screen);
    }

    @Override
    void init() {
        buttons.clear();
        int left = screen.width / 2 - HALF + 4;
        int right = screen.width / 2 + HALF - 4;
        int top = screen.top();

        // 自分のヒット音: 1 行目はプリセット（◀ と ▶ を両端に。名前は draw で間に描く）
        int y = top + 49;
        add(new GuiButton(BUTTON_PRESET_PREV, left, y, 20, 20, "◀"));
        add(new GuiButton(BUTTON_PRESET_NEXT, right - 20, y, 20, 20, "▶"));

        // 2 行目はマイプリセットへの保存
        y += 22;
        int slotsLeft = left + screen.font().getStringWidth(I18n.format("weakspot.sound.preset.saveTo")) + 8;
        int slotWidth = (right - slotsLeft - 4 * (SoundPreset.MY_SLOTS - 1)) / SoundPreset.MY_SLOTS;
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            saveSlots[i] = add(new GuiButton(BUTTON_SAVE_SLOT + i, slotsLeft + i * (slotWidth + 4), y, slotWidth, 20,
                    ""));
        }

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
        y = top + 175;
        othersSound = add(new GuiButton(BUTTON_OTHERS_SOUND, left, y, 110, 20, ""));
        add(new VolumeSlider(BUTTON_OTHERS_VOLUME, left + 114, y, WeakSpotConfig.client.sound.othersHitVolume, v -> {
            WeakSpotConfig.client.sound.othersHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOtherFlat(1);
        }));
        add(new GuiButton(BUTTON_OTHERS_PREVIEW, left + 238, y, right - left - 238, 20,
                I18n.format("weakspot.sound.preview")));

        confirmSlot = 0;
        shownIndex = matchIndex();
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
        if (!shown && confirmSlot != 0) {
            confirmSlot = 0;
            updateLabels();
        }
    }

    @Override
    boolean action(int id) {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        if (id >= BUTTON_SAVE_SLOT && id < BUTTON_SAVE_SLOT + SoundPreset.MY_SLOTS) {
            saveTo(id - BUTTON_SAVE_SLOT + 1);
            return true;
        }
        // 保存の枠以外のボタンを押したら、上書きの確かめはやめる
        if (confirmSlot != 0 && buttons.stream().anyMatch(b -> b.id == id)) {
            confirmSlot = 0;
        }
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
            case BUTTON_MY_PREVIEW:
                updateLabels();
                HitSounds.clear();
                HitSounds.playScale(HitSounds::playOwn, PREVIEW_TICKS_PER_NOTE, 0);
                return true;
            case BUTTON_OTHERS_PREVIEW:
                updateLabels();
                HitSounds.clear();
                HitSounds.playScale(HitSounds::playOtherFlat, PREVIEW_TICKS_PER_NOTE, 0);
                return true;
            default:
                return false;
        }
    }

    /**
     * マイプリセットの枠 slot に保存する。空きならすぐ保存。保存済みなら、1 回目は「上書き？」にして、確かめ中に
     * もう一度押したら上書きする。保存したら「マイプリセット n に保存しました」を 2 秒出す。
     */
    private void saveTo(int slot) {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        boolean saved = SoundPreset.mySlot(sound.soundPresets, slot) != null;
        if (saved && !(confirmSlot == slot && Minecraft.getSystemTime() - confirmMs < CONFIRM_MS)) {
            confirmSlot = slot;
            confirmMs = Minecraft.getSystemTime();
            updateLabels();
            return;
        }
        sound.soundPresets = SoundPreset.withMySlot(sound.soundPresets, slot, SoundPreset.of(sound));
        WeakSpotConfig.save();
        confirmSlot = 0;
        savedSlot = slot;
        savedMs = Minecraft.getSystemTime();
        shownIndex = SoundPreset.BUILT_IN.size() + slot - 1;
        updateLabels();
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
        savedMs = Long.MIN_VALUE / 2;
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
        savedMs = Long.MIN_VALUE / 2;
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
        for (int i = 0; i < SoundPreset.MY_SLOTS; i++) {
            int slot = i + 1;
            GuiButton button = saveSlots[i];
            if (slot == confirmSlot) {
                button.displayString = I18n.format("weakspot.sound.preset.overwrite");
                button.packedFGColour = CONFIRM_RGB;
            } else if (SoundPreset.mySlot(sound.soundPresets, slot) == null) {
                button.displayString = I18n.format("weakspot.sound.preset.empty", slot);
                button.packedFGColour = EMPTY_RGB;
            } else {
                button.displayString = Integer.toString(slot);
                button.packedFGColour = 0;
            }
        }
    }

    /** プリセットの位置の名前（空きのマイプリセットは「（空き）」付き、-1 はカスタム）。 */
    private static String presetName(int index) {
        if (index < 0) {
            return I18n.format("weakspot.sound.preset.custom");
        }
        if (index < SoundPreset.BUILT_IN.size()) {
            return I18n.format("weakspot.sound.preset." + SoundPreset.BUILT_IN.get(index).key);
        }
        int slot = index - SoundPreset.BUILT_IN.size() + 1;
        String name = I18n.format("weakspot.sound.preset.my", slot);
        return SoundPreset.mySlot(WeakSpotConfig.client.sound.soundPresets, slot) == null
                ? I18n.format("weakspot.sound.preset.empty", name) : name;
    }

    private static boolean emptySlotShown(int index) {
        return index >= SoundPreset.BUILT_IN.size() && presetAt(index) == null;
    }

    private static String instrumentLabel(HitSound sound) {
        return I18n.format("weakspot.sound.instrument", I18n.format("weakspot.sound.instrument." + sound.name()));
    }

    @Override
    void draw() {
        long now = Minecraft.getSystemTime();
        if (confirmSlot != 0 && now - confirmMs >= CONFIRM_MS) {
            // 上書きの確かめは 3 秒で元に戻す
            confirmSlot = 0;
            updateLabels();
        }
        int center = screen.width / 2;
        int left = center - HALF + 4;
        int right = center + HALF - 4;
        int top = screen.top();
        // 自分の音とほかの人の音の枠（ボタンより先に描くので、ボタンの後ろになる）
        box(center - HALF, top + 37, center + HALF, top + 161);
        box(center - HALF, top + 164, center + HALF, top + 199);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.mine"), left, top + 39, 0xFFFFFF);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.others"), left, top + 166, 0xFFFFFF);

        // プリセットの名前（◀ と ▶ の間）。保存した直後の 2 秒は「保存しました」
        String label;
        int color;
        if (now - savedMs < SAVED_MS) {
            label = I18n.format("weakspot.sound.preset.saved", I18n.format("weakspot.sound.preset.my", savedSlot));
            color = SAVED_RGB;
        } else {
            label = presetName(shownIndex);
            color = emptySlotShown(shownIndex) ? EMPTY_RGB : 0xFFFFFF;
        }
        int room = right - left - 48;
        if (screen.font().getStringWidth(label) > room) {
            label = screen.font().trimStringToWidth(label, room - 6) + "…";
        }
        screen.drawCenteredString(screen.font(), label, center, top + 55, color);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.preset.saveTo"), left, top + 77, 0xFFFFFF);

        if (screen.bottom() - top >= NOTE_NEEDS_HEIGHT) {
            screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.note"), center, top + 202, 0xAAAAAA);
        }
        // コンボの和音の説明は、和音のボタンにマウスを乗せたとき
        int mx = screen.mouseX();
        int my = screen.mouseY();
        if (chord.visible && mx >= chord.x && mx < chord.x + chord.width && my >= chord.y
                && my < chord.y + chord.height) {
            screen.setTooltip(I18n.format("weakspot.sound.comboNote"));
        }
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

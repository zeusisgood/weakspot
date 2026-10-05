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
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * 統計画面の「サウンド」タブ（1.8.9 で StatsScreen から分けた）。ヒット音の楽器と音量、音階（1.11.1）を変えて試聴する。
 * 1.11.1 から、自分の音とほかの人の音を枠で分け、1 行目に音のプリセットのボタン「プリセット: 標準」（OptiFine の
 * シェーダーのプロファイルと同じ形。左クリックで次、右クリックか Shift + 左クリックで前）。プリセット名は今の設定から求め
 * （どれにも一致しなければ「カスタム」）、選んだものは保存しない。マイプリセットの読み込み・保存は MyPresetsScreen。
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
    /** プリセット（1.11.1）と、マイプリセットの画面を開く。 */
    private static final int BUTTON_PRESET = 50;
    private static final int BUTTON_MY_PRESETS = 51;
    /** 試聴で音階を鳴らす間隔（tick）。 */
    private static final int PREVIEW_TICKS_PER_NOTE = 4;
    /** 音階の設定を切り替えたときの短い試聴（音の数と間隔 tick）。往復なら上がって 1 つ下がるまで分かる数。 */
    private static final int SWITCH_PREVIEW_NOTES = 5;
    private static final int SWITCH_PREVIEW_TICKS = 2;
    /** 枠（半透明の黒 #000000 と、枠線の灰 #555555）と、見出しの文字（灰 #AAAAAA）。 */
    static final int BOX_FILL = 0x60000000;
    static final int BOX_LINE = 0xFF555555;
    static final int SUBTLE_RGB = 0xAAAAAA;
    /** 中身の幅の半分（タブのボタンの列と同じ）。 */
    private static final int HALF = 154;
    /** マイプリセットの画面を開くボタンの幅。 */
    private static final int MY_PRESETS_WIDTH = 96;
    /** 下の説明を出すのに要る、上から下のボタンまでの高さ（小さい画面では出さない）。 */
    private static final int NOTE_NEEDS_HEIGHT = 206;

    private final List<GuiButton> buttons = new ArrayList<>();
    private GuiButton preset;
    private GuiButton myPresets;
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
        int left = screen.width / 2 - HALF + 4;
        int right = screen.width / 2 + HALF - 4;
        int top = screen.top();

        // 自分のヒット音: 1 行目はプリセットと、マイプリセットの画面
        int y = top + 49;
        preset = add(new GuiButton(BUTTON_PRESET, left, y, right - left - MY_PRESETS_WIDTH - 4, 20, ""));
        myPresets = add(new GuiButton(BUTTON_MY_PRESETS, right - MY_PRESETS_WIDTH, y, MY_PRESETS_WIDTH, 20,
                I18n.format("weakspot.sound.myPresets")));

        // 「細かい設定」の見出し（draw で描く）の下
        y += 33;
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
        y = top + 167;
        othersSound = add(new GuiButton(BUTTON_OTHERS_SOUND, left, y, 110, 20, ""));
        add(new VolumeSlider(BUTTON_OTHERS_VOLUME, left + 114, y, WeakSpotConfig.client.sound.othersHitVolume, v -> {
            WeakSpotConfig.client.sound.othersHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOtherFlat(1);
        }));
        add(new GuiButton(BUTTON_OTHERS_PREVIEW, left + 238, y, right - left - 238, 20,
                I18n.format("weakspot.sound.preview")));

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
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        switch (id) {
            case BUTTON_PRESET:
                choosePreset(GuiScreen.isShiftKeyDown() ? -1 : 1);
                return true;
            case BUTTON_MY_PRESETS:
                Minecraft.getMinecraft().displayGuiScreen(new MyPresetsScreen(screen));
                return true;
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

    /** プリセットのボタンの右クリックは「前へ」（バニラのボタンは右クリックを拾わないので、ここで受ける）。 */
    @Override
    void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mouseButton == 1 && preset.visible && preset.enabled && preset.mousePressed(mc, mouseX, mouseY)) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            choosePreset(-1);
        }
    }

    /**
     * 巡る順（最初から入っているもの → 保存してあるマイプリセット。空きは飛ばす）。
     */
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

    /** 今の設定と一致する巡る順の位置（最初から入っているものを先に探す。なければ -1 = カスタム）。 */
    private static int matchIndex(List<SoundPreset> cycle) {
        SoundPreset current = SoundPreset.of(WeakSpotConfig.client.sound);
        for (int i = 0; i < cycle.size(); i++) {
            if (current.sameSound(cycle.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** 次（step = 1）か前（-1）のプリセットにして保存し、短く鳴らす。カスタムからは、次なら最初、前なら最後へ。 */
    private void choosePreset(int step) {
        List<SoundPreset> cycle = cycle();
        int index = matchIndex(cycle);
        int next = index < 0 ? (step > 0 ? 0 : cycle.size() - 1) : Math.floorMod(index + step, cycle.size());
        cycle.get(next).applyTo(WeakSpotConfig.client.sound);
        WeakSpotConfig.save();
        playSwitchPreview();
        updateLabels();
    }

    /** 楽器・和音を変えた: 保存して、表示を直す。 */
    private void onSoundChanged() {
        WeakSpotConfig.save();
        updateLabels();
    }

    /** 音階の設定を切り替えた: 保存して、新しい設定の始めの数音を速めに鳴らす（1.11.1。続けて押したら前の試聴を止める）。 */
    private void onScaleChanged() {
        onSoundChanged();
        playSwitchPreview();
    }

    /** 今の設定の始めの数音を速めに鳴らす。往復は、上がりきる手前から鳴らして、折り返しが分かるようにする。 */
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
        preset.displayString = I18n.format("weakspot.sound.preset", currentPresetName());
        mySound.displayString = instrumentLabel(sound.myHitSound);
        othersSound.displayString = instrumentLabel(sound.othersHitSound);
        chord.displayString = I18n.format("weakspot.sound.chordShort",
                I18n.format(sound.hitChordEnabled ? "options.on" : "options.off"));
        scaleDirection.displayString = I18n.format("weakspot.sound.scaleDirection",
                I18n.format("weakspot.sound.scaleDirection." + sound.hitScaleDirection.name()));
        scaleType.displayString = I18n.format("weakspot.sound.scaleType",
                I18n.format("weakspot.sound.scaleType." + sound.hitScaleType.name()));
        scaleOctaves.displayString = I18n.format("weakspot.sound.scaleOctaves", sound.hitScaleOctaves);
    }

    /** 今の設定のプリセット名（最初から入っているもの → マイプリセット n → カスタム）。 */
    private static String currentPresetName() {
        SoundPreset current = SoundPreset.of(WeakSpotConfig.client.sound);
        for (SoundPreset p : SoundPreset.BUILT_IN) {
            if (current.sameSound(p)) {
                return I18n.format("weakspot.sound.preset." + p.key);
            }
        }
        for (int slot = 1; slot <= SoundPreset.MY_SLOTS; slot++) {
            if (current.sameSound(SoundPreset.mySlot(WeakSpotConfig.client.sound.soundPresets, slot))) {
                return I18n.format("weakspot.sound.preset.my", slot);
            }
        }
        return I18n.format("weakspot.sound.preset.custom");
    }

    private static String instrumentLabel(HitSound sound) {
        return I18n.format("weakspot.sound.instrument", I18n.format("weakspot.sound.instrument." + sound.name()));
    }

    @Override
    void draw() {
        int center = screen.width / 2;
        int left = center - HALF + 4;
        int right = center + HALF - 4;
        int top = screen.top();
        // 自分の音とほかの人の音の枠（ボタンより先に描くので、ボタンの後ろになる）
        box(center - HALF, top + 37, center + HALF, top + 152);
        box(center - HALF, top + 156, center + HALF, top + 191);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.mine"), left, top + 39, 0xFFFFFF);
        screen.drawString(screen.font(), I18n.format("weakspot.sound.others"), left, top + 158, 0xFFFFFF);
        // 「細かい設定」の見出しと細い線
        String fine = I18n.format("weakspot.sound.fineSettings");
        int textY = top + 72;
        screen.drawString(screen.font(), fine, left, textY, SUBTLE_RGB);
        int lineX = left + screen.font().getStringWidth(fine) + 4;
        Gui.drawRect(lineX, textY + 4, right, textY + 5, BOX_LINE);

        if (screen.bottom() - top >= NOTE_NEEDS_HEIGHT) {
            screen.drawCenteredString(screen.font(), I18n.format("weakspot.sound.note"), center, top + 195, SUBTLE_RGB);
        }
        int mx = screen.mouseX();
        int my = screen.mouseY();
        if (over(preset, mx, my)) {
            screen.setTooltip(I18n.format("weakspot.sound.preset.tooltip"));
        } else if (over(myPresets, mx, my)) {
            screen.setTooltip(I18n.format("weakspot.sound.myPresets.tooltip"));
        } else if (over(chord, mx, my)) {
            // コンボの和音の説明は、和音のボタンにマウスを乗せたとき
            screen.setTooltip(I18n.format("weakspot.sound.comboNote"));
        }
    }

    private static boolean over(GuiButton button, int mx, int my) {
        return button.visible && mx >= button.x && mx < button.x + button.width && my >= button.y
                && my < button.y + button.height;
    }

    /** 薄い枠（半透明の黒と、灰の枠線）。 */
    static void box(int left, int top, int right, int bottom) {
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

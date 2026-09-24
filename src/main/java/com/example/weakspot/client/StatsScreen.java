package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MiningStats;
import com.example.weakspot.config.HitSound;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.StatsRequestMessage;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;

/**
 * 統計画面（K キー）。「統計」タブは、サーバーから届いた「今回」と「累計」を並べて表示するだけ。
 * 「サウンド」タブは、ヒット音の楽器と音量を変えて試聴する部品で、クライアントだけで完結する（サーバーとは通信しない）。
 * 値は weakspot.cfg にそのまま保存するので、Forge の設定画面と同じ値になる。
 */
final class StatsScreen extends GuiScreen {

    private static final int BUTTON_RESET = 0;
    private static final int BUTTON_DONE = 1;
    private static final int BUTTON_CONFIG = 2;
    private static final int BUTTON_TAB_STATS = 10;
    private static final int BUTTON_TAB_SOUND = 11;
    private static final int BUTTON_MY_SOUND = 20;
    private static final int BUTTON_MY_VOLUME = 21;
    private static final int BUTTON_MY_PREVIEW = 22;
    private static final int BUTTON_OTHERS_SOUND = 30;
    private static final int BUTTON_OTHERS_VOLUME = 31;
    private static final int BUTTON_OTHERS_PREVIEW = 32;

    /** 13 行（見出し + 統計 13 項目）が、一番小さい画面でも下のボタンに重ならない高さ。 */
    private static final int ROW_HEIGHT = 11;
    /** 試聴で音階を鳴らす間隔（tick）。 */
    private static final int PREVIEW_TICKS_PER_NOTE = 4;

    /** サーバーから最後に届いた統計。届くまでは null。 */
    private static MiningStats session;
    private static MiningStats total;
    /** 最後に開いていたタブ（画面を開き直しても保つ）。 */
    private static boolean soundTab;

    private GuiButton resetButton;
    private GuiButton tabStats;
    private GuiButton tabSound;
    private GuiButton mySound;
    private GuiButton othersSound;
    private boolean confirmingReset;
    private int top;
    /** 今のフレームの、マウスが乗っている行の説明（統計タブ）。 */
    private String tooltip;
    private int mouseX;
    private int mouseY;

    /** サーバーから統計が届いた（クライアントのスレッドで呼ぶ）。 */
    static void receive(MiningStats newSession, MiningStats newTotal) {
        session = newSession;
        total = newTotal;
    }

    /** 画面を開くとき。前のワールドの数字を出さないように、届くまでは空欄にする。 */
    static void open(Minecraft mc) {
        session = null;
        total = null;
        mc.displayGuiScreen(new StatsScreen());
    }

    /** initGui は設定画面から戻ったときにも呼ばれるので、そのたびに統計をもらい直し、設定の値も読み直す。 */
    @Override
    public void initGui() {
        WeakSpotMod.network.sendToServer(new StatsRequestMessage(false));

        buttonList.clear();
        top = Math.max(10, height / 2 - 116);
        int center = width / 2;

        tabStats = add(new GuiButton(BUTTON_TAB_STATS, center - 102, top + 14, 100, 20,
                I18n.format("weakspot.stats.tab.stats")));
        tabSound = add(new GuiButton(BUTTON_TAB_SOUND, center + 2, top + 14, 100, 20,
                I18n.format("weakspot.stats.tab.sound")));

        int y = top + 56;
        mySound = add(new GuiButton(BUTTON_MY_SOUND, center - 154, y, 120, 20, ""));
        add(new VolumeSlider(BUTTON_MY_VOLUME, center - 30, y, WeakSpotConfig.myHitVolume, v -> {
            WeakSpotConfig.myHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOwn(1);
        }));
        add(new GuiButton(BUTTON_MY_PREVIEW, center + 94, y, 60, 20, I18n.format("weakspot.sound.preview")));

        y += 48;
        othersSound = add(new GuiButton(BUTTON_OTHERS_SOUND, center - 154, y, 120, 20, ""));
        add(new VolumeSlider(BUTTON_OTHERS_VOLUME, center - 30, y, WeakSpotConfig.othersHitVolume, v -> {
            WeakSpotConfig.othersHitVolume = v;
            WeakSpotConfig.save();
            HitSounds.playOtherFlat(1);
        }));
        add(new GuiButton(BUTTON_OTHERS_PREVIEW, center + 94, y, 60, 20, I18n.format("weakspot.sound.preview")));

        int bottom = Math.min(height - 28, top + 200);
        resetButton = add(new GuiButton(BUTTON_RESET, center - 154, bottom, 100, 20, ""));
        add(new GuiButton(BUTTON_CONFIG, center - 50, bottom, 100, 20, I18n.format("weakspot.stats.openConfig")));
        add(new GuiButton(BUTTON_DONE, center + 54, bottom, 100, 20, I18n.format("gui.done")));

        setConfirmingReset(false);
        updateLabels();
        showTab(soundTab);
    }

    private <T extends GuiButton> T add(T button) {
        buttonList.add(button);
        return button;
    }

    private void showTab(boolean sound) {
        soundTab = sound;
        tabStats.enabled = sound;
        tabSound.enabled = !sound;
        for (GuiButton button : buttonList) {
            if (button.id >= BUTTON_MY_SOUND) {
                button.visible = sound;
            }
        }
        resetButton.visible = !sound;
        setConfirmingReset(false);
    }

    private void updateLabels() {
        mySound.displayString = instrumentLabel(WeakSpotConfig.myHitSound);
        othersSound.displayString = instrumentLabel(WeakSpotConfig.othersHitSound);
    }

    private static String instrumentLabel(HitSound sound) {
        return I18n.format("weakspot.sound.instrument", I18n.format("weakspot.sound.instrument." + sound.name()));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_RESET:
                if (confirmingReset) {
                    WeakSpotMod.network.sendToServer(new StatsRequestMessage(true));
                    setConfirmingReset(false);
                } else {
                    setConfirmingReset(true);
                }
                break;
            case BUTTON_DONE:
                mc.displayGuiScreen(null);
                break;
            case BUTTON_CONFIG:
                mc.displayGuiScreen(WeakSpotGuiFactory.create(this));
                break;
            case BUTTON_TAB_STATS:
                showTab(false);
                break;
            case BUTTON_TAB_SOUND:
                showTab(true);
                break;
            case BUTTON_MY_SOUND:
                WeakSpotConfig.myHitSound = WeakSpotConfig.myHitSound.next();
                WeakSpotConfig.save();
                updateLabels();
                HitSounds.playOwn(1);
                break;
            case BUTTON_OTHERS_SOUND:
                WeakSpotConfig.othersHitSound = WeakSpotConfig.othersHitSound.next();
                WeakSpotConfig.save();
                updateLabels();
                HitSounds.playOtherFlat(1);
                break;
            case BUTTON_MY_PREVIEW:
                HitSounds.clear();
                HitSounds.playScale(HitSounds::playOwn, PREVIEW_TICKS_PER_NOTE, 0);
                break;
            case BUTTON_OTHERS_PREVIEW:
                HitSounds.clear();
                HitSounds.playScale(HitSounds::playOtherFlat, PREVIEW_TICKS_PER_NOTE, 0);
                break;
            default:
                break;
        }
    }

    /** 累計のリセットは2回押しで確定する（誤操作防止）。 */
    private void setConfirmingReset(boolean confirming) {
        confirmingReset = confirming;
        resetButton.displayString = I18n.format(confirming ? "weakspot.stats.reset.confirm" : "weakspot.stats.reset");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        tooltip = null;
        drawCenteredString(fontRenderer, I18n.format("weakspot.stats.title"), width / 2, top, 0xFFFFFF);
        if (soundTab) {
            drawSoundTab();
        } else {
            drawStatsTab();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tooltip != null) {
            drawHoveringText(fontRenderer.listFormattedStringToWidth(tooltip, 200), mouseX, mouseY);
        }
    }

    private void drawSoundTab() {
        int labelX = width / 2 - 154;
        drawString(fontRenderer, I18n.format("weakspot.sound.mine"), labelX, top + 44, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("weakspot.sound.others"), labelX, top + 92, 0xFFFFFF);
        drawCenteredString(fontRenderer, I18n.format("weakspot.sound.note"), width / 2, top + 132, 0xAAAAAA);
    }

    private void drawStatsTab() {
        int labelX = width / 2 - 150;
        int sessionRight = width / 2 + 70;
        int totalRight = width / 2 + 150;
        int y = top + 42;
        drawRight(I18n.format("weakspot.stats.column.session"), sessionRight, y, 0xAAAAAA);
        drawRight(I18n.format("weakspot.stats.column.total"), totalRight, y, 0xAAAAAA);
        y += ROW_HEIGHT;

        y = row("weakspot.stats.hits", s -> Long.toString(s.hits), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.blocks", s -> Long.toString(s.blocksBroken), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.blocksWithHit", s -> Long.toString(s.blocksBrokenWithHit), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.averageHits", StatsScreen::formatAverage, labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.maxHits", s -> Long.toString(s.maxHitsOnBlock), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.timeSaved", s -> formatDuration(s.savedSeconds()), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.growthHits", s -> Long.toString(s.growthHits), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.machineHits", s -> Long.toString(s.machineHits), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.animalHits", s -> Long.toString(s.animalHits), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.fishingHits", s -> Long.toString(s.fishingHits), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.bowHits", s -> Long.toString(s.bowHits), labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.critHits", s -> Long.toString(s.critHits), labelX, sessionRight, totalRight, y);
        row("weakspot.stats.maxStreak", s -> Long.toString(s.maxStreak), labelX, sessionRight, totalRight, y);
    }

    private int row(String labelKey, Function<MiningStats, String> value, int labelX, int sessionRight, int totalRight,
                    int y) {
        drawString(fontRenderer, I18n.format(labelKey), labelX, y, 0xFFFFFF);
        String tooltipKey = labelKey + ".tooltip";
        if (I18n.hasKey(tooltipKey) && mouseY >= y && mouseY < y + ROW_HEIGHT && mouseX >= labelX
                && mouseX <= totalRight) {
            tooltip = I18n.format(tooltipKey);
        }
        drawRight(session == null ? "..." : value.apply(session), sessionRight, y, 0xFFFFFF);
        drawRight(total == null ? "..." : value.apply(total), totalRight, y, 0xFFFF55);
        return y + ROW_HEIGHT;
    }

    private void drawRight(String text, int right, int y, int color) {
        drawString(fontRenderer, text, right - fontRenderer.getStringWidth(text), y, color);
    }

    private static String formatAverage(MiningStats stats) {
        double average = stats.averageHitsPerBlock();
        return Double.isNaN(average) ? "-" : String.format("%.2f", average);
    }

    private static String formatDuration(double seconds) {
        if (seconds < 60) {
            return I18n.format("weakspot.stats.time.seconds", String.format("%.1f", seconds));
        }
        long whole = (long) seconds;
        if (whole < 3600) {
            return I18n.format("weakspot.stats.time.minutes", whole / 60, String.format("%02d", whole % 60));
        }
        return I18n.format("weakspot.stats.time.hours", whole / 3600, String.format("%02d", whole / 60 % 60));
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

package com.example.weakspot.client;

import com.example.weakspot.GuideBook;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerColor;
import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.common.MiningStats;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.HitSound;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.StatsRequestMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * 統計画面（K キー）。「統計」タブは、サーバーから届いた「今回」と「累計」を並べて表示するだけ。
 * 「サウンド」タブは、ヒット音の楽器と音量を変えて試聴する部品で、クライアントだけで完結する（サーバーとは通信しない）。
 * 「弱点マーカー」タブ（1.7.0。1.8.3 で「弱点」から改名し、色と形に ◀ を足した）は、種類ごとのオン・オフ（KindSwitches。サーバーにも伝わる）と、自分の弱点の色と形（MarkerLook）。
 * 値は weakspot.cfg にそのまま保存するので、Forge の設定画面と同じ値になる。
 * 行が画面に入りきらないタブ（統計・弱点）は、マウスのホイールで送る。
 */
final class StatsScreen extends GuiScreen {

    private static final int BUTTON_RESET = 0;
    private static final int BUTTON_DONE = 1;
    private static final int BUTTON_CONFIG = 2;
    /** ガイドの本を開く（1.4.1）。どちらのタブでも出す。 */
    private static final int BUTTON_GUIDE = 3;
    private static final int BUTTON_TAB_STATS = 10;
    private static final int BUTTON_TAB_SOUND = 11;
    private static final int BUTTON_TAB_KINDS = 12;
    /** 「弱点マーカー」タブの行のボタン（+ 種類の番号）。 */
    private static final int BUTTON_KIND_TOGGLE = 100;
    private static final int BUTTON_KIND_COLOR = 200;
    private static final int BUTTON_KIND_SHAPE = 300;
    /** 色と形を 1 つ前に戻す ◀ と、形の ▶（1.8.3）。 */
    private static final int BUTTON_KIND_COLOR_PREV = 400;
    private static final int BUTTON_KIND_SHAPE_PREV = 500;
    private static final int BUTTON_KIND_SHAPE_NEXT = 600;
    private static final int KIND_ROW_HEIGHT = 22;
    private static final int TAB_STATS = 0;
    private static final int TAB_SOUND = 1;
    private static final int TAB_KINDS = 2;
    private static final int BUTTON_MY_SOUND = 20;
    private static final int BUTTON_MY_VOLUME = 21;
    private static final int BUTTON_MY_PREVIEW = 22;
    private static final int BUTTON_OTHERS_SOUND = 30;
    private static final int BUTTON_OTHERS_VOLUME = 31;
    private static final int BUTTON_OTHERS_PREVIEW = 32;

    /** 統計の 1 行の高さ。入りきらない行は、ホイールで送る。 */
    private static final int ROW_HEIGHT = 10;
    /** 試聴で音階を鳴らす間隔（tick）。 */
    private static final int PREVIEW_TICKS_PER_NOTE = 4;

    /** サーバーから最後に届いた統計。届くまでは null。 */
    private static MiningStats session;
    private static MiningStats total;
    /** 最後に開いていたタブ（画面を開き直しても保つ）。 */
    private static int tab = TAB_STATS;

    private GuiButton resetButton;
    private GuiButton tabStats;
    private GuiButton tabSound;
    private GuiButton tabKinds;
    /** 統計・弱点のタブの、送った行の数。 */
    private int statsScroll;
    private int kindsScroll;
    private int bottom;
    private final GuiTextField[] colorFields = new GuiTextField[HitKind.values().length];
    private final boolean[] colorInvalid = new boolean[HitKind.values().length];
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
        top = Math.max(6, height / 2 - 124);
        int center = width / 2;

        tabStats = add(new GuiButton(BUTTON_TAB_STATS, center - 154, top + 14, 74, 20,
                I18n.format("weakspot.stats.tab.stats")));
        tabSound = add(new GuiButton(BUTTON_TAB_SOUND, center - 76, top + 14, 74, 20,
                I18n.format("weakspot.stats.tab.sound")));
        tabKinds = add(new GuiButton(BUTTON_TAB_KINDS, center + 2, top + 14, 74, 20,
                I18n.format("weakspot.stats.tab.kinds")));
        add(new GuiButton(BUTTON_GUIDE, center + 80, top + 14, 74, 20, I18n.format("weakspot.stats.guide")));

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

        bottom = Math.min(height - 28, top + 226);
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            // 1 行: [名前][オン/オフ][■][◀][入力欄][▶] [◀][形][▶]（1.8.3。幅は center ± 154 に収める）
            add(new GuiButton(BUTTON_KIND_TOGGLE + i, center - 64, 0, 36, 20, ""));
            add(new GuiButton(BUTTON_KIND_COLOR_PREV + i, center - 14, 0, 12, 20, "\u25C0"));
            add(new GuiButton(BUTTON_KIND_COLOR + i, center + 50, 0, 12, 20, "\u25B6"));
            add(new GuiButton(BUTTON_KIND_SHAPE_PREV + i, center + 66, 0, 12, 20, "\u25C0"));
            add(new GuiButton(BUTTON_KIND_SHAPE + i, center + 80, 0, 54, 20, ""));
            add(new GuiButton(BUTTON_KIND_SHAPE_NEXT + i, center + 136, 0, 12, 20, "\u25B6"));
            GuiTextField field = new GuiTextField(BUTTON_KIND_COLOR + 50 + i, fontRenderer, center, 0, 48, 18);
            field.setMaxStringLength(7);
            Integer custom = MarkerLook.customColor(kind);
            field.setText(custom == null ? "" : String.format("#%06X", custom));
            colorFields[i] = field;
            colorInvalid[i] = false;
        }

        resetButton = add(new GuiButton(BUTTON_RESET, center - 154, bottom, 100, 20, ""));
        add(new GuiButton(BUTTON_CONFIG, center - 50, bottom, 100, 20, I18n.format("weakspot.stats.openConfig")));
        add(new GuiButton(BUTTON_DONE, center + 54, bottom, 100, 20, I18n.format("gui.done")));

        setConfirmingReset(false);
        updateLabels();
        showTab(tab);
    }

    private <T extends GuiButton> T add(T button) {
        buttonList.add(button);
        return button;
    }

    private void showTab(int newTab) {
        tab = newTab;
        tabStats.enabled = tab != TAB_STATS;
        tabSound.enabled = tab != TAB_SOUND;
        tabKinds.enabled = tab != TAB_KINDS;
        for (GuiButton button : buttonList) {
            if (button.id >= BUTTON_MY_SOUND && button.id < BUTTON_KIND_TOGGLE) {
                button.visible = tab == TAB_SOUND;
            }
        }
        resetButton.visible = tab == TAB_STATS;
        setConfirmingReset(false);
        layoutKindRows();
    }

    /** 画面に入る「弱点マーカー」タブの行の数。 */
    private int visibleKindRows() {
        return Math.max(1, (bottom - 4 - (top + 52)) / KIND_ROW_HEIGHT);
    }

    /** 「弱点マーカー」タブの行のボタンを、今の送り位置に並べる（見えない行は隠す）。 */
    private void layoutKindRows() {
        int rows = visibleKindRows();
        kindsScroll = Math.max(0, Math.min(kindsScroll, HitKind.values().length - rows));
        SyncedSettings settings = ClientSettings.get();
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            int row = i - kindsScroll;
            boolean shown = tab == TAB_KINDS && row >= 0 && row < rows;
            int y = top + 52 + row * KIND_ROW_HEIGHT;
            boolean server = isEnabledOnServer(kind, settings);
            for (GuiButton button : buttonList) {
                if (button.id == BUTTON_KIND_TOGGLE + i) {
                    button.visible = shown;
                    button.y = y;
                    button.enabled = server;
                } else if (button.id == BUTTON_KIND_COLOR + i || button.id == BUTTON_KIND_SHAPE + i
                        || button.id == BUTTON_KIND_COLOR_PREV + i || button.id == BUTTON_KIND_SHAPE_PREV + i
                        || button.id == BUTTON_KIND_SHAPE_NEXT + i) {
                    // サーバーで無効な種類は、色と形を隠して「サーバーで無効」と出す
                    button.visible = shown && server;
                    button.y = y;
                }
            }
            colorFields[i].y = y + 1;
            colorFields[i].setVisible(shown && server);
            colorFields[i].setEnabled(server);
        }
        updateKindLabels();
    }

    private void updateKindLabels() {
        for (HitKind kind : HitKind.values()) {
            int i = kind.ordinal();
            for (GuiButton button : buttonList) {
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

    /** サーバーの設定で、その種類の弱点が出るか（出ない種類は「サーバーで無効」と出して、押せなくする）。 */
    private static boolean isEnabledOnServer(HitKind kind, SyncedSettings settings) {
        switch (kind) {
            case FISHING: return settings.fishingWeakSpotEnabled;
            case BOW: return settings.bowWeakSpotEnabled;
            case MELEE: return settings.meleeWeakSpotEnabled;
            case VEHICLE: return settings.vehicleWeakSpotEnabled;
            case EAT: return settings.eatWeakSpotEnabled;
            case SLEEP: return settings.sleepWeakSpotEnabled;
            case LADDER: return settings.ladderWeakSpotEnabled;
            case ELYTRA: return settings.elytraWeakSpotEnabled;
            case ENCHANT: return settings.enchantWeakSpotEnabled;
            case HARVEST: return settings.harvestWeakSpotEnabled;
            case THROW: return settings.throwWeakSpotEnabled;
            case SPRINT: return settings.sprintWeakSpotEnabled;
            case PORTAL: return settings.portalWeakSpotEnabled;
            default: return true;
        }
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
        updateKindLabels();
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
            case BUTTON_GUIDE:
                // 持っていなくても読めるように、その場で作った本を開く（サーバーには何も送らない）
                mc.displayGuiScreen(new GuiScreenBook(mc.player, GuideBook.create(), false));
                break;
            case BUTTON_TAB_STATS:
                showTab(TAB_STATS);
                break;
            case BUTTON_TAB_SOUND:
                showTab(TAB_SOUND);
                break;
            case BUTTON_TAB_KINDS:
                showTab(TAB_KINDS);
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
                kindButton(button.id);
                break;
        }
    }

    /** 「弱点マーカー」タブの行のボタン。 */
    private void kindButton(int id) {
        HitKind[] kinds = HitKind.values();
        if (id >= BUTTON_KIND_TOGGLE && id < BUTTON_KIND_TOGGLE + kinds.length) {
            KindSwitches.toggle(kinds[id - BUTTON_KIND_TOGGLE]);
            updateKindLabels();
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
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (tab == TAB_KINDS) {
            for (HitKind kind : HitKind.values()) {
                GuiTextField field = colorFields[kind.ordinal()];
                if (field.getVisible() && field.isFocused()) {
                    if (keyCode == Keyboard.KEY_ESCAPE) {
                        field.setFocused(false);
                        return;
                    }
                    if (field.textboxKeyTyped(typedChar, keyCode)) {
                        applyTypedColor(kind);
                    }
                    if (keyCode == Keyboard.KEY_RETURN) {
                        field.setFocused(false);
                    }
                    return;
                }
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (tab == TAB_KINDS) {
            for (GuiTextField field : colorFields) {
                if (field.getVisible()) {
                    field.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        for (GuiTextField field : colorFields) {
            field.updateCursorCounter();
        }
    }

    /** 行が入りきらないタブは、ホイールで送る。 */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int step = wheel > 0 ? -1 : 1;
        if (tab == TAB_KINDS) {
            kindsScroll += step;
            layoutKindRows();
        } else if (tab == TAB_STATS) {
            statsScroll = Math.max(0, statsScroll + step);
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
        if (tab == TAB_SOUND) {
            drawSoundTab();
        } else if (tab == TAB_KINDS) {
            drawKindsTab();
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

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {"weakspot.stats.hits", (Function<MiningStats, String>) s -> Long.toString(s.hits)});
        rows.add(new Object[] {"weakspot.stats.blocks", (Function<MiningStats, String>) s -> Long.toString(s.blocksBroken)});
        rows.add(new Object[] {"weakspot.stats.blocksWithHit",
                (Function<MiningStats, String>) s -> Long.toString(s.blocksBrokenWithHit)});
        rows.add(new Object[] {"weakspot.stats.averageHits", (Function<MiningStats, String>) StatsScreen::formatAverage});
        rows.add(new Object[] {"weakspot.stats.maxHits", (Function<MiningStats, String>) s -> Long.toString(s.maxHitsOnBlock)});
        rows.add(new Object[] {"weakspot.stats.timeSaved",
                (Function<MiningStats, String>) s -> formatDuration(s.savedSeconds())});
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                rows.add(new Object[] {statsKey(kind), (Function<MiningStats, String>) s -> Long.toString(s.count(kind))});
            }
        }
        rows.add(new Object[] {"weakspot.stats.maxStreak", (Function<MiningStats, String>) s -> Long.toString(s.maxStreak)});
        rows.add(new Object[] {"weakspot.stats.totalHits", (Function<MiningStats, String>) s -> Long.toString(s.totalHits())});

        int fit = Math.max(1, (bottom - 4 - y) / ROW_HEIGHT);
        statsScroll = Math.max(0, Math.min(statsScroll, rows.size() - fit));
        for (int i = statsScroll; i < Math.min(rows.size(), statsScroll + fit); i++) {
            @SuppressWarnings("unchecked")
            Function<MiningStats, String> value = (Function<MiningStats, String>) rows.get(i)[1];
            y = row((String) rows.get(i)[0], value, labelX, sessionRight, totalRight, y);
        }
        drawScrollHint(statsScroll > 0, statsScroll + fit < rows.size());
    }

    /** 種類ごとのヒット数の翻訳キー（近接はクリティカル数）。 */
    private static String statsKey(HitKind kind) {
        return kind == HitKind.MELEE ? "weakspot.stats.critHits" : "weakspot.stats." + kind.key() + "Hits";
    }

    /** まだ上・下に行があるとき、右端に ▲ ▼ を出す（ホイールで送れる）。 */
    private void drawScrollHint(boolean up, boolean down) {
        int x = width / 2 + 158;
        if (up) {
            drawString(fontRenderer, "\u25B2", x, top + 52, 0xAAAAAA);
        }
        if (down) {
            drawString(fontRenderer, "\u25BC", x, bottom - 14, 0xAAAAAA);
        }
    }

    private void drawKindsTab() {
        int center = width / 2;
        drawString(fontRenderer, I18n.format("weakspot.kinds.column.kind"), center - 154, top + 40, 0xAAAAAA);
        drawString(fontRenderer, I18n.format("weakspot.kinds.column.onOff"), center - 64, top + 40, 0xAAAAAA);
        drawString(fontRenderer, I18n.format("weakspot.kinds.column.color"), center - 26, top + 40, 0xAAAAAA);
        drawString(fontRenderer, I18n.format("weakspot.kinds.column.shape"), center + 66, top + 40, 0xAAAAAA);
        if (!WeakSpotConfig.weakSpotsEnabled) {
            // 題の行の右に出す（列の見出しと重ならないように）
            drawRight(I18n.format("weakspot.kinds.pausedAll", ToggleKeyHandler.keyName()), center + 154, top,
                    0xFFFF55);
        }
        SyncedSettings settings = ClientSettings.get();
        int rows = visibleKindRows();
        for (HitKind kind : HitKind.values()) {
            int row = kind.ordinal() - kindsScroll;
            if (row < 0 || row >= rows) {
                continue;
            }
            int y = top + 52 + row * KIND_ROW_HEIGHT;
            boolean server = isEnabledOnServer(kind, settings);
            drawString(fontRenderer, I18n.format("weakspot.kind." + kind.key()), center - 154, y + 6,
                    server ? 0xFFFFFF : 0x808080);
            if (!server) {
                drawString(fontRenderer, I18n.format("weakspot.kinds.serverDisabled"), center - 26, y + 6, 0x808080);
                continue;
            }
            int rgb = MarkerLook.color(kind, MarkerLook.defaultColor(kind));
            // 見本（今の色）
            drawRect(center - 26, y + 5, center - 16, y + 15, 0xFF000000 | rgb);
            GuiTextField field = colorFields[kind.ordinal()];
            field.x = center;
            field.drawTextBox();
            if (colorInvalid[kind.ordinal()]) {
                int x0 = field.x - 1;
                int y0 = field.y - 1;
                drawHorizontalLine(x0, x0 + field.width + 1, y0, 0xFFFF4D4D);
                drawHorizontalLine(x0, x0 + field.width + 1, y0 + field.height + 1, 0xFFFF4D4D);
                drawVerticalLine(x0, y0, y0 + field.height + 1, 0xFFFF4D4D);
                drawVerticalLine(x0 + field.width + 1, y0, y0 + field.height + 1, 0xFFFF4D4D);
            } else if (field.getText().isEmpty() && !field.isFocused()) {
                drawString(fontRenderer, I18n.format("weakspot.kinds.default"), field.x + 4, field.y + 5, 0x808080);
            }
        }
        drawScrollHint(kindsScroll > 0, kindsScroll + rows < HitKind.values().length);
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

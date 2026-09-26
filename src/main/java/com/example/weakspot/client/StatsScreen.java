package com.example.weakspot.client;

import com.example.weakspot.GuideBook;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MiningStats;
import com.example.weakspot.network.StatsRequestMessage;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Mouse;

/**
 * 統計画面（K キー。題は「弱点破壊」）の共通の枠: 題、タブの切り替え、「ガイド」「設定画面を開く」「完了」、
 * ホイールの送り、説明の吹き出し。タブの中身は、それぞれのクラス（1.8.9 で分けた）:
 * 「統計」StatsTab、「サウンド」SoundTab、「弱点マーカー」KindsTab。
 */
final class StatsScreen extends GuiScreen {

    private static final int BUTTON_DONE = 1;
    private static final int BUTTON_CONFIG = 2;
    /** ガイドの本を開く（1.4.1）。どのタブでも出す。 */
    private static final int BUTTON_GUIDE = 3;
    private static final int BUTTON_TAB_STATS = 10;
    private static final int BUTTON_TAB_SOUND = 11;
    private static final int BUTTON_TAB_KINDS = 12;
    private static final int TAB_STATS = 0;
    private static final int TAB_SOUND = 1;
    private static final int TAB_KINDS = 2;

    /** 最後に開いていたタブ（画面を開き直しても保つ）。 */
    private static int tab = TAB_STATS;

    private final StatsScreenTab[] tabs = {new StatsTab(this), new SoundTab(this), new KindsTab(this)};
    private GuiButton[] tabButtons;
    private int top;
    private int bottom;
    /** 今のフレームの、マウスが乗っている行の説明（統計タブ）。 */
    private String tooltip;
    private int mouseX;
    private int mouseY;

    /** サーバーから統計が届いた（クライアントのスレッドで呼ぶ）。 */
    static void receive(MiningStats newSession, MiningStats newTotal) {
        StatsTab.receive(newSession, newTotal);
    }

    /** 画面を開くとき。前のワールドの数字を出さないように、届くまでは空欄にする。 */
    static void open(Minecraft mc) {
        StatsTab.forget();
        mc.displayGuiScreen(new StatsScreen());
    }

    /** initGui は設定画面から戻ったときにも呼ばれるので、そのたびに統計をもらい直し、設定の値も読み直す。 */
    @Override
    public void initGui() {
        WeakSpotMod.network.sendToServer(new StatsRequestMessage(false));

        buttonList.clear();
        top = Math.max(6, height / 2 - 124);
        bottom = Math.min(height - 28, top + 226);
        int center = width / 2;

        tabButtons = new GuiButton[] {
                add(new GuiButton(BUTTON_TAB_STATS, center - 154, top + 14, 74, 20,
                        I18n.format("weakspot.stats.tab.stats"))),
                add(new GuiButton(BUTTON_TAB_SOUND, center - 76, top + 14, 74, 20,
                        I18n.format("weakspot.stats.tab.sound"))),
                add(new GuiButton(BUTTON_TAB_KINDS, center + 2, top + 14, 74, 20,
                        I18n.format("weakspot.stats.tab.kinds"))),
        };
        add(new GuiButton(BUTTON_GUIDE, center + 80, top + 14, 74, 20, I18n.format("weakspot.stats.guide")));
        for (StatsScreenTab t : tabs) {
            t.init();
        }
        add(new GuiButton(BUTTON_CONFIG, center - 50, bottom, 100, 20, I18n.format("weakspot.stats.openConfig")));
        add(new GuiButton(BUTTON_DONE, center + 54, bottom, 100, 20, I18n.format("gui.done")));
        showTab(tab);
    }

    <T extends GuiButton> T add(T button) {
        buttonList.add(button);
        return button;
    }

    private void showTab(int newTab) {
        tab = newTab;
        for (int i = 0; i < tabs.length; i++) {
            tabButtons[i].enabled = i != tab;
            tabs[i].show(i == tab);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
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
            default:
                tabs[tab].action(button.id);
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!tabs[tab].keyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        tabs[tab].mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        for (StatsScreenTab t : tabs) {
            t.update();
        }
    }

    /** 行が入りきらないタブは、ホイールで送る。 */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            tabs[tab].scroll(wheel > 0 ? -1 : 1);
        }
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
        tabs[tab].draw();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tooltip != null) {
            drawHoveringText(fontRenderer.listFormattedStringToWidth(tooltip, 200), mouseX, mouseY);
        }
    }

    // タブが使う、画面の共通の部品

    int top() {
        return top;
    }

    /** 下のボタンの行の y（タブの行はこれより上に収める）。 */
    int bottom() {
        return bottom;
    }

    FontRenderer font() {
        return fontRenderer;
    }

    int mouseX() {
        return mouseX;
    }

    int mouseY() {
        return mouseY;
    }

    void setTooltip(String text) {
        tooltip = text;
    }

    void drawRight(String text, int right, int y, int color) {
        drawString(fontRenderer, text, right - fontRenderer.getStringWidth(text), y, color);
    }

    /** まだ上・下に行があるとき、右端に ▲ ▼ を出す（ホイールで送れる）。 */
    void drawScrollHint(boolean up, boolean down) {
        int x = width / 2 + 158;
        if (up) {
            drawString(fontRenderer, "▲", x, top + 52, 0xAAAAAA);
        }
        if (down) {
            drawString(fontRenderer, "▼", x, bottom - 14, 0xAAAAAA);
        }
    }
}

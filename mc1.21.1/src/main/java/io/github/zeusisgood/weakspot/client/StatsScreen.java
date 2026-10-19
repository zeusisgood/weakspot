package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.GuideBook;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.network.StatsRequestMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** 統計画面（K キー）の共通の枠。タブの中身は StatsScreenTab を継いだクラス。 */
final class StatsScreen extends Screen {

    private static final int TAB_STATS = 0;
    private static final int TAB_SOUND = 1;
    private static final int TAB_KINDS = 2;
    private static final int TAB_TARGET = 3;

    /** 画面を開き直しても保つ。 */
    private static int tab = TAB_STATS;

    private final StatsScreenTab[] tabs = {new StatsTab(this), new SoundTab(this), new KindsTab(this),
            new TargetTab(this)};
    private Button[] tabButtons;
    private int top;
    private int bottom;
    /** 統計タブで、マウスが乗っている行の説明。 */
    private String tooltip;
    private int mouseX;
    private int mouseY;

    private StatsScreen() {
        super(Component.translatable("weakspot.stats.title"));
    }

    /** クライアントのスレッドで呼ぶ。 */
    static void receive(MiningStats newSession, MiningStats newTotal) {
        StatsTab.receive(newSession, newTotal);
    }

    /** 前のワールドの数字を出さないように、届くまでは空欄にする。 */
    static void open(Minecraft mc) {
        StatsTab.forget();
        mc.setScreen(new StatsScreen());
    }

    /** 設定画面から戻ったときにも呼ばれるので、そのたびに統計をもらい直し、設定の値も読み直す。 */
    @Override
    protected void init() {
        WeakSpotMod.network.sendToServer(new StatsRequestMessage(false));

        top = Math.max(6, height / 2 - 124);
        bottom = Math.min(height - 28, top + 226);
        int center = width / 2;

        tabButtons = new Button[] {
                add(button("weakspot.stats.tab.stats", center - 154, top + 14, 60, b -> showTab(TAB_STATS))),
                add(button("weakspot.stats.tab.sound", center - 92, top + 14, 60, b -> showTab(TAB_SOUND))),
                add(button("weakspot.stats.tab.kinds", center - 30, top + 14, 60, b -> showTab(TAB_KINDS))),
                add(button("weakspot.stats.tab.target", center + 32, top + 14, 60, b -> showTab(TAB_TARGET))),
        };
        // 持っていなくても読めるように、その場で作った本を開く（サーバーには何も送らない）
        add(button("weakspot.stats.guide", center + 94, top + 14, 60,
                b -> minecraft.setScreen(new BookViewScreen(BookViewScreen.BookAccess.fromItem(GuideBook.create())))));
        for (StatsScreenTab t : tabs) {
            t.init();
        }
        add(button("weakspot.stats.openConfig", center - 50, bottom, 100,
                b -> minecraft.setScreen(WeakSpotGuiFactory.create(this))));
        add(button("gui.done", center + 54, bottom, 100, b -> minecraft.setScreen(null)));
        showTab(tab);
    }

    /** 高さ 20 のボタン。 */
    static Button button(String key, int x, int y, int width, Button.OnPress onPress) {
        return Button.builder(Component.literal(I18n.get(key)), onPress).bounds(x, y, width, 20).build();
    }

    <T extends AbstractWidget> T add(T widget) {
        return addRenderableWidget(widget);
    }

    /** 入力は受けるが、描くのはタブ（draw の中で、ほかの絵と同じ順に描く）。 */
    <T extends AbstractWidget> T addInput(T widget) {
        return addWidget(widget);
    }

    private void showTab(int newTab) {
        tab = newTab;
        for (int i = 0; i < tabs.length; i++) {
            tabButtons[i].active = i != tab;
            tabs[i].show(i == tab);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return tabs[tab].keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean handled = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!handled && getFocused() instanceof EditBox) {
            // 1.12.2 の入力欄と同じく、ほかの所を押したら入力を終える
            setFocused(null);
        }
        tabs[tab].mouseClicked((int) mouseX, (int) mouseY, mouseButton);
        return handled;
    }

    @Override
    public void tick() {
        for (StatsScreenTab t : tabs) {
            t.update();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean handled = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (scrollY != 0) {
            tabs[tab].scroll(scrollY > 0 ? -1 : 1);
        }
        return handled;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** タブの絵をボタンより先に描くため、super.render（背景 → ボタン）を分けて呼ぶ。 */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        tooltip = null;
        g.drawCenteredString(font, I18n.get("weakspot.stats.title"), width / 2, top, 0xFFFFFFFF);
        tabs[tab].draw(g);
        for (Renderable renderable : renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }
        if (tooltip != null) {
            g.renderTooltip(font, font.split(Component.literal(tooltip), 200), mouseX, mouseY);
        }
    }

    int top() {
        return top;
    }

    /** タブの行はこれより上に収める。 */
    int bottom() {
        return bottom;
    }

    Font font() {
        return font;
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

    /** rgb は 0xRRGGBB。 */
    void drawString(GuiGraphics g, String text, int x, int y, int rgb) {
        g.drawString(font, text, x, y, 0xFF000000 | rgb);
    }

    void drawCenteredString(GuiGraphics g, String text, int x, int y, int rgb) {
        g.drawCenteredString(font, text, x, y, 0xFF000000 | rgb);
    }

    void drawRight(GuiGraphics g, String text, int right, int y, int rgb) {
        drawString(g, text, right - font.width(text), y, rgb);
    }

    void drawScrollHint(GuiGraphics g, boolean up, boolean down) {
        int x = width / 2 + 158;
        if (up) {
            drawString(g, "▲", x, top + 52, 0xAAAAAA);
        }
        if (down) {
            drawString(g, "▼", x, bottom - 14, 0xAAAAAA);
        }
    }
}

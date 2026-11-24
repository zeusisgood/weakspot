package io.github.zeusisgood.weakspot.client;

import net.minecraft.client.gui.GuiGraphics;

/** 画面の共通の枠（題・タブ・下のボタン・ホイール・説明の吹き出し）は StatsScreen が持つ。 */
abstract class StatsScreenTab {

    final StatsScreen screen;

    StatsScreenTab(StatsScreen screen) {
        this.screen = screen;
    }

    /** 設定画面から戻ったときも、画面を作り直すたびに呼ぶ。ボタンを screen.add で足す（押したときの処理はボタンに持たせる）。 */
    abstract void init();

    abstract void show(boolean shown);

    /** ボタンより先に呼ぶので、ここで描いたものはボタンの後ろになる。 */
    abstract void draw(GuiGraphics g);

    /** 下が +1。 */
    void scroll(int step) {
    }

    /** 受け取ったら true（画面の既定の処理、たとえば Esc で閉じる、より先に呼ぶ）。 */
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    void update() {
    }
}

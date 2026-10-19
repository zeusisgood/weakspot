package io.github.zeusisgood.weakspot.client;

import java.io.IOException;

/** 画面の共通の枠（題・タブ・下のボタン・ホイール・説明の吹き出し）は StatsScreen が持つ。 */
abstract class StatsScreenTab {

    final StatsScreen screen;

    StatsScreenTab(StatsScreen screen) {
        this.screen = screen;
    }

    /** 設定画面から戻ったときも、画面を作り直すたびに呼ぶ。ボタンを screen.add で足す。 */
    abstract void init();

    abstract void show(boolean shown);

    /** このタブのボタンなら処理して true。 */
    boolean action(int id) {
        return false;
    }

    abstract void draw();

    /** 下が +1。 */
    void scroll(int step) {
    }

    /** 受け取ったら true。 */
    boolean keyTyped(char typedChar, int keyCode) throws IOException {
        return false;
    }

    void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    void update() {
    }
}

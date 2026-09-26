package com.example.weakspot.client;

import java.io.IOException;

/**
 * 統計画面（StatsScreen）の 1 つのタブ（1.8.9 で StatsScreen から分けた）。ボタンは init で画面に足し、見せる・隠すは
 * show。画面の共通の枠（題・タブ・下のボタン・ホイール・説明の吹き出し）は StatsScreen が持つ。
 */
abstract class StatsScreenTab {

    final StatsScreen screen;

    StatsScreenTab(StatsScreen screen) {
        this.screen = screen;
    }

    /** 画面を作り直すたび（設定画面から戻ったときも）。ボタンを screen.add で足す。 */
    abstract void init();

    /** このタブを出す・隠す。 */
    abstract void show(boolean shown);

    /** このタブのボタンなら処理して true。 */
    boolean action(int id) {
        return false;
    }

    abstract void draw();

    /** ホイール（step は 1 行ずつ。下が +1）。 */
    void scroll(int step) {
    }

    /** 文字の入力を受け取ったら true。 */
    boolean keyTyped(char typedChar, int keyCode) throws IOException {
        return false;
    }

    void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    void update() {
    }
}

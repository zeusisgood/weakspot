package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * 画面（GUI）の上に出して、カーソルで当てるマーカーの 1 つの種類（1.8.8。共通の土台）。当て方は、重ねるだけ（睡眠。1.10.1）か、
 * クリック（エンチャント。重ねるだけだと、候補を選ぶ途中で引き直してしまうため）。描く・当てる流れは ScreenSpots が
 * 回し、種類ごとに違うこと（出す画面と条件、置ける範囲、色、当てたあと、画面の注釈）だけを、ここを継いだクラスが書く。
 * 使う種類: 睡眠（SleepSpot）・エンチャント（EnchantSpot）。
 */
abstract class ScreenSpotKind {

    /** 基本の半径（GUI の座標）。実際の半径は radius()（1.11.0 から設定 spotSize を掛ける）。 */
    private static final double RADIUS = 12;

    final HitKind kind;
    final Random random = new Random();
    final MarkerMotion motion = new MarkerMotion(0, 0);

    /** 出している画面（開き直したら新しい位置に出す）。 */
    GuiScreen shownOn;
    /** 今の位置。置ける場所がなければ has が false。 */
    boolean has;
    double x;
    double y;

    ScreenSpotKind(HitKind kind) {
        this.kind = kind;
    }

    /** 半径（GUI の座標。設定 spotSize と種類ごとの倍率を掛けたもの）。 */
    final double radius() {
        return RADIUS * ClientSettings.get().spotSize(kind);
    }

    /** この画面で、この種類を出すか（種類がオン・設定・画面の種類など）。false の間は、出している画面を忘れる。 */
    abstract boolean eligible(Minecraft mc, GuiScreen gui);

    /** マーカーを描いて、当ててよいか（エンチャントは候補が出ている間だけ）。 */
    boolean markerVisible(GuiScreen gui) {
        return has;
    }

    /** カーソルを重ねるだけで当たりか（1.10.1）。false ならクリックで当てる（当てたクリックはボタンなどに届かない）。 */
    boolean hitsOnHover() {
        return false;
    }

    /** ヒットの最小間隔（設定の表から。1.8.9）。 */
    final int minHitInterval(SyncedSettings settings) {
        return settings.minHitInterval(kind);
    }

    /** 前の位置 (prevX, prevY) から離れたランダムな位置を x, y に入れる。置ける場所がなければ false。 */
    abstract boolean place(GuiScreen gui, double prevX, double prevY);

    /** 円・輪・中心の色 {disk, ring, center}。 */
    abstract float[][] look();

    /** 輪と中心の点の濃さ。 */
    abstract float outlineAlpha();

    /** マーカーのあとに、画面に描くもの（エンチャントの枠の上の注釈）。 */
    void drawExtra(Minecraft mc, GuiScreen gui) {
    }
}

package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * 画面（GUI）の上でカーソルで当てるマーカーの種類の土台。描く・当てる流れは ScreenSpots が回す。
 * 当て方は、重ねるだけ（睡眠）か、クリック（エンチャント。重ねるだけだと、候補を選ぶ途中で引き直してしまうため）。
 */
abstract class ScreenSpotKind {

    /** GUI の座標。実際の半径は radius()。 */
    private static final double RADIUS = 12;

    final HitKind kind;
    final Random random = new Random();
    final MarkerMotion motion = new MarkerMotion(0, 0);

    /** 開き直したら新しい位置に出す。 */
    GuiScreen shownOn;
    /** 置ける場所がなければ false。 */
    boolean has;
    double x;
    double y;

    ScreenSpotKind(HitKind kind) {
        this.kind = kind;
    }

    final double radius() {
        return RADIUS * ClientSettings.get().spotSize(kind);
    }

    /** false の間は、出している画面を忘れる。 */
    abstract boolean eligible(Minecraft mc, GuiScreen gui);

    /** エンチャントは候補が出ている間だけ。 */
    boolean markerVisible(GuiScreen gui) {
        return has;
    }

    /** false ならクリックで当てる（当てたクリックはボタンなどに届かない）。 */
    boolean hitsOnHover() {
        return false;
    }

    final int minHitInterval(SyncedSettings settings) {
        return settings.minHitInterval(kind);
    }

    /** 置ける場所がなければ false。 */
    abstract boolean place(GuiScreen gui, double prevX, double prevY);

    /**
    * 前の位置から minMove 以上離れた点がなければ、一番離れた点。hole {左, 右, 上, 下} の中の点は使わない（null なら除かない）。
    * 使える点が 1 つもなければ、x, y を変えずに false。
    */
    final boolean placeRandom(double minX, double maxX, double minY, double maxY, double[] hole, int tries,
            double minMove, double prevX, double prevY) {
        boolean found = false;
        double bestX = 0;
        double bestY = 0;
        double bestMove = -1;
        for (int i = 0; i < tries; i++) {
            double nx = minX + random.nextDouble() * (maxX - minX);
            double ny = minY + random.nextDouble() * (maxY - minY);
            if (hole != null && nx > hole[0] && nx < hole[1] && ny > hole[2] && ny < hole[3]) {
                continue;
            }
            double move = Math.hypot(nx - prevX, ny - prevY);
            if (move >= minMove) {
                x = nx;
                y = ny;
                return true;
            }
            if (move > bestMove) {
                found = true;
                bestMove = move;
                bestX = nx;
                bestY = ny;
            }
        }
        if (found) {
            x = bestX;
            y = bestY;
        }
        return found;
    }

    /** {disk, ring, center} を返す。 */
    abstract float[][] look();

    /** 輪と中心の点の濃さ。 */
    abstract float outlineAlpha();

    void drawExtra(Minecraft mc, GuiScreen gui) {
    }
}

package com.example.weakspot.common;

import java.util.Random;

/**
 * 釣りの弱点の計算（SPEC_v1.2 §1.10）。Minecraft に依存しない。
 * 待ち時間の換算、弱点の次の位置（浮きのまわり）、許す角度（画面上の円の半径から）、進み具合。
 */
public final class FishingMath {

    /** バニラの最長の待ち時間（tick。ルアーなしで 100〜600）。ヒット数に換算する基準。 */
    public static final int MAX_WAIT_TICKS = 600;
    /** 弱点が動ける範囲（浮きからの水平距離、ブロック）。 */
    public static final double SPOT_RADIUS = 1.0;
    /** ヒットのあと、前の位置から最低でも離れる距離（ブロック）。 */
    public static final double MIN_MOVE = 0.5;
    /** 画面上の弱点の円の半径（GUI のピクセル）。 */
    public static final double SPOT_SCREEN_RADIUS = 12.0;

    private FishingMath() {
    }

    /** 1ヒットで減らす待ち時間（tick）。ヒット数が 0 以下の設定は無効として 0。 */
    public static int ticksPerHit(int hits) {
        return hits <= 0 ? 0 : Math.max(1, MAX_WAIT_TICKS / hits);
    }

    /**
     * ヒット後の待ち時間の残り。0 を下回らない。バニラは待ち時間が 0 のとき次の待ち時間を引き直すので、
     * 待ち時間の途中では 1 を残し、バニラが次の tick に「魚が寄ってくる段階」へ進める。
     */
    public static int waitAfterHit(int remaining, int ticks) {
        return Math.max(1, remaining - ticks);
    }

    /** 待ち時間の進み具合（0.0〜1.0）。initial は待ち時間が始まったときの長さ、remaining は今の残り。 */
    public static double progress(int initial, int remaining) {
        if (initial <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, 1.0 - (double) remaining / initial));
    }

    /**
     * 弱点の次の位置（浮きからの水平の差 {dx, dz}）。浮きから SPOT_RADIUS 以内で、前の位置 (prevDx, prevDz) から
     * MIN_MOVE 以上離れる。
     */
    public static double[] nextSpot(double prevDx, double prevDz, Random random) {
        double[] best = {SPOT_RADIUS, 0};
        double bestDistance = -1;
        for (int i = 0; i < 40; i++) {
            // 円の中で一様に選ぶ
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = SPOT_RADIUS * Math.sqrt(random.nextDouble());
            double[] p = {r * Math.cos(angle), r * Math.sin(angle)};
            double d = Math.hypot(p[0] - prevDx, p[1] - prevDz);
            if (d >= MIN_MOVE) {
                return p;
            }
            if (d > bestDistance) {
                bestDistance = d;
                best = p;
            }
        }
        return best;
    }

    /**
     * 照準が重なっているとする、視線と「目から弱点への向き」の角度の差の上限（ラジアン）。
     * 画面の中央から radiusPx だけ離れた点が、ちょうど境界になる。fovDegrees は縦の視野角、screenHeightPx は画面の高さ
     * （radiusPx と同じ単位のピクセル）。
     */
    public static double allowedAngle(double radiusPx, double fovDegrees, double screenHeightPx) {
        double halfHeight = screenHeightPx / 2.0;
        return Math.atan(radiusPx * Math.tan(Math.toRadians(fovDegrees) / 2.0) / halfHeight);
    }

    /** 照準が重なっているか。 */
    public static boolean isAimed(double angleRadians, double allowedRadians) {
        return angleRadians <= allowedRadians;
    }
}

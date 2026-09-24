package com.example.weakspot.common;

import java.util.Random;

/**
 * 弓の弱点の計算（SPEC_v1.3 §1、過剰チャージは SPEC_v1.3.4 §2）。Minecraft に依存しない。
 * 弱点の向き（yaw / pitch。Minecraft と同じ度の向き）、引きの進め方、引きゲージの値、過剰チャージの倍率。
 */
public final class BowMath {

    /** バニラの弓を引き切るまでの時間（tick）。ゲージと「引き切った」の基準。 */
    public static final int FULL_DRAW_TICKS = 20;
    /** 弱点を出す、視線からずらす角度の範囲（度）。 */
    public static final double MIN_OFFSET_DEGREES = 3.0;
    public static final double MAX_OFFSET_DEGREES = 8.0;
    /** ヒットのあと、前の向きから最低でも離れる角度（度）。 */
    public static final double MIN_MOVE_DEGREES = 4.0;
    /** 過剰チャージ（引き切ったあとのヒット）1回で上げる、矢のダメージの割合（1.3.4）。 */
    public static final double OVERCHARGE_PER_HIT = 0.10;
    /** 過剰チャージの上限のヒット数（+50%）。 */
    public static final int MAX_OVERCHARGE_HITS = 5;

    private BowMath() {
    }

    /** 向き（yaw, pitch。度）の単位ベクトル。Minecraft の Entity#getVectorForRotation と同じ向き。 */
    public static double[] vector(double yaw, double pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new double[] {-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    /** 単位ベクトルの向き {yaw, pitch}（度）。vector の逆。 */
    public static double[] rotation(double[] d) {
        double pitch = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -d[1]))));
        double yaw = Math.toDegrees(Math.atan2(-d[0], d[2]));
        return new double[] {yaw, pitch};
    }

    /** 2つの向きの間の角度（度）。 */
    public static double angleBetween(double yaw1, double pitch1, double yaw2, double pitch2) {
        double[] a = vector(yaw1, pitch1);
        double[] b = vector(yaw2, pitch2);
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
    }

    /**
     * 次の弱点の向き {yaw, pitch}（度）。今の視線 (lookYaw, lookPitch) から MIN_OFFSET〜MAX_OFFSET 度ずれた向きで、
     * 前の向き (prevYaw, prevPitch) から MIN_MOVE 度以上離れる。最初の弱点は prev に視線を渡す。
     * yaw は、前の向きとの差が ±180 度に収まるように返す（表示の移動が遠回りしないように）。
     */
    public static double[] nextSpot(double lookYaw, double lookPitch, double prevYaw, double prevPitch,
                                    Random random) {
        double[] look = vector(lookYaw, lookPitch);
        // 視線に垂直な2本の軸
        double[] up = Math.abs(look[1]) > 0.99 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
        double[] side = normalize(cross(look, up));
        double[] top = cross(side, look);
        double[] best = null;
        double bestMove = -1;
        for (int i = 0; i < 40; i++) {
            double offset = Math.toRadians(MIN_OFFSET_DEGREES
                    + random.nextDouble() * (MAX_OFFSET_DEGREES - MIN_OFFSET_DEGREES));
            double around = random.nextDouble() * 2 * Math.PI;
            double s = Math.sin(offset);
            double c = Math.cos(offset);
            double[] d = new double[3];
            for (int k = 0; k < 3; k++) {
                d[k] = look[k] * c + (side[k] * Math.cos(around) + top[k] * Math.sin(around)) * s;
            }
            double[] r = rotation(normalize(d));
            r[0] = prevYaw + wrapDegrees(r[0] - prevYaw);
            double move = angleBetween(r[0], r[1], prevYaw, prevPitch);
            if (move >= MIN_MOVE_DEGREES) {
                return r;
            }
            if (move > bestMove) {
                bestMove = move;
                best = r;
            }
        }
        return best;
    }

    /** 1ヒットで実際に進める tick 数。引いた時間 used に足して、引き切り（FULL_DRAW_TICKS）を超えない。 */
    public static int addedTicks(int used, int hitTicks) {
        return Math.max(0, Math.min(hitTicks, FULL_DRAW_TICKS - used));
    }

    /** 過剰チャージ hits 回の矢のダメージの倍率（上限 MAX_OVERCHARGE_HITS 回）。 */
    public static double overchargeMultiplier(int hits) {
        return 1.0 + OVERCHARGE_PER_HIT * Math.max(0, Math.min(hits, MAX_OVERCHARGE_HITS));
    }

    /** まだ過剰チャージできるか（引き切ったあとも弱点を出すか）。 */
    public static boolean canOvercharge(int hits) {
        return hits < MAX_OVERCHARGE_HITS;
    }

    /** 引き切ったか。 */
    public static boolean isFull(int used) {
        return used >= FULL_DRAW_TICKS;
    }

    /** 引きゲージの値（0.0〜1.0）。used は引いた時間（tick。フレームの途中の値でもよい）。 */
    public static double barValue(double used) {
        return Math.max(0.0, Math.min(1.0, used / FULL_DRAW_TICKS));
    }

    /** 角度を -180〜180 度にする。 */
    public static double wrapDegrees(double degrees) {
        double d = degrees % 360.0;
        if (d >= 180.0) {
            d -= 360.0;
        }
        if (d < -180.0) {
            d += 360.0;
        }
        return d;
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / length, v[1] / length, v[2] / length};
    }
}

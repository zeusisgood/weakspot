package com.example.weakspot.common;

import java.util.Random;

/**
 * 弓の弱点の計算（SPEC_v1.3 §1、過剰チャージは SPEC_v1.3.4 §2）。Minecraft に依存しない。
 * 弱点の向き（yaw / pitch。Minecraft と同じ度の向き）、引きの進め方、引きゲージの値、過剰チャージの倍率。
 */
public final class BowMath {

    /** バニラの弓を引き切るまでの時間（tick）。ゲージと「引き切った」の基準。 */
    public static final int FULL_DRAW_TICKS = 20;
    /** 弱点を出す、視線からずらす角度の範囲（度）。1.5.4 で 3〜8 から広げた（近すぎて照準を動かさずに当たったため）。 */
    public static final double MIN_OFFSET_DEGREES = 10.0;
    public static final double MAX_OFFSET_DEGREES = 20.0;
    /** ずらす角度の上限を、縦の視野角の半分のこの割合までに抑える（画面からはみ出さないように）。 */
    public static final double MAX_OFFSET_FOV_FRACTION = 0.7;
    /** ヒットのあと、前の向きから最低でも離れる角度（度）。 */
    public static final double MIN_MOVE_DEGREES = 8.0;
    /**
     * 照準が水平からこの角度（度）より上か下を向いていたら、次の弱点は水平に戻る側に出す（1.8.1。当て続けても
     * 照準が真上・真下まで行かないように）。この範囲の中では、前の弱点と反対側に出す（交互）。
     */
    public static final double HORIZON_BAND_DEGREES = 20.0;
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
     * ずらす角度の範囲 {下限, 上限}（度）。上限は min(MAX_OFFSET, 縦の視野角の半分 × MAX_OFFSET_FOV_FRACTION)、
     * 下限は min(MIN_OFFSET, 上限の半分)。視野角が分からない（0 以下）ときは、そのままの範囲。
     */
    public static double[] offsetRange(double fovDegrees) {
        double max = MAX_OFFSET_DEGREES;
        if (fovDegrees > 0) {
            max = Math.min(max, fovDegrees / 2 * MAX_OFFSET_FOV_FRACTION);
        }
        return new double[] {Math.min(MIN_OFFSET_DEGREES, max / 2), max};
    }

    /** 視野角を考えない版（MIN_OFFSET〜MAX_OFFSET）。 */
    public static double[] nextSpot(double lookYaw, double lookPitch, double prevYaw, double prevPitch,
                                    Random random) {
        return nextSpot(lookYaw, lookPitch, prevYaw, prevPitch, 0, random);
    }

    /**
     * 次の弱点の向き {yaw, pitch}（度）。今の視線 (lookYaw, lookPitch) から MIN_OFFSET〜MAX_OFFSET 度ずれた向きで、
     * 前の向き (prevYaw, prevPitch) から MIN_MOVE 度以上離れる。最初の弱点は prev に視線を渡す。
     * ずらす角度は、縦の視野角 fovDegrees で抑える（offsetRange）。
     * yaw は、前の向きとの差が ±180 度に収まるように返す（表示の移動が遠回りしないように）。
     */
    public static double[] nextSpot(double lookYaw, double lookPitch, double prevYaw, double prevPitch,
                                    double fovDegrees, Random random) {
        return nextSpot(lookYaw, lookPitch, prevYaw, prevPitch, 0, 0, fovDegrees, random);
    }

    /**
     * 向きを決めた次の弱点の向き（1.8.1）。yawSign は視線より yaw が増える側（+1）か減る側（-1）、pitchSign は視線より
     * 下（+1。pitch が増える）か上（-1）。0 ならどちらでもよい。決めた側の、10〜20 度の輪の 4 分の 1（斜めを含む）に出す。
     * その側に前の向きから離れた所が取れなければ、一番離れた所。
     */
    public static double[] nextSpot(double lookYaw, double lookPitch, double prevYaw, double prevPitch,
                                    int yawSign, int pitchSign, double fovDegrees, Random random) {
        double[] range = offsetRange(fovDegrees);
        double[] look = vector(lookYaw, lookPitch);
        // 視線に垂直な2本の軸
        double[] up = Math.abs(look[1]) > 0.99 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
        double[] side = normalize(cross(look, up));
        double[] top = cross(side, look);
        double[] best = null;
        double bestMove = -1;
        double[] anyBest = null;
        for (int i = 0; i < 200; i++) {
            double offset = Math.toRadians(range[0] + random.nextDouble() * (range[1] - range[0]));
            double around = random.nextDouble() * 2 * Math.PI;
            double s = Math.sin(offset);
            double c = Math.cos(offset);
            double[] d = new double[3];
            for (int k = 0; k < 3; k++) {
                d[k] = look[k] * c + (side[k] * Math.cos(around) + top[k] * Math.sin(around)) * s;
            }
            double[] r = rotation(normalize(d));
            r[0] = prevYaw + wrapDegrees(r[0] - prevYaw);
            if (anyBest == null) {
                anyBest = r;
            }
            if (!onSide(wrapDegrees(r[0] - lookYaw), yawSign) || !onSide(r[1] - lookPitch, pitchSign)) {
                continue;
            }
            double move = angleBetween(r[0], r[1], prevYaw, prevPitch);
            if (move >= MIN_MOVE_DEGREES) {
                return r;
            }
            if (move > bestMove) {
                bestMove = move;
                best = r;
            }
        }
        return best != null ? best : anyBest;
    }

    /** delta が sign の側か（sign が 0 なら、どちらでもよい）。 */
    private static boolean onSide(double delta, int sign) {
        return sign == 0 || delta * sign >= 0;
    }

    /**
     * 次の弱点を、照準の上（-1）と下（+1）のどちらに出すか（1.8.1）。keepNearHorizon なら、照準が水平から
     * HORIZON_BAND_DEGREES より上を向いていれば下、下を向いていれば上（水平に戻る側）。そうでなければ、
     * 前の側 prevSign の反対（交互）。最初（prevSign が 0）はランダム。エリトラは keepNearHorizon を false にする。
     */
    public static int nextPitchSign(double lookPitch, int prevSign, boolean keepNearHorizon, Random random) {
        if (keepNearHorizon) {
            if (lookPitch > HORIZON_BAND_DEGREES) {
                return -1;
            }
            if (lookPitch < -HORIZON_BAND_DEGREES) {
                return 1;
            }
        }
        return alternate(prevSign, random);
    }

    /** 次の弱点を、照準の左右のどちらに出すか（1.8.1。前の側の反対。最初はランダム）。 */
    public static int nextYawSign(int prevSign, Random random) {
        return alternate(prevSign, random);
    }

    private static int alternate(int prevSign, Random random) {
        if (prevSign == 0) {
            return random.nextBoolean() ? 1 : -1;
        }
        return -Integer.signum(prevSign);
    }

    /**
     * 照準の真上か真下だけに出す弱点の次の pitch（度。1.6.0。馬・豚に乗っているとき）。yaw は使う側が視線に合わせる。
     * 視線から offsetRange の角度だけ上か下（ランダム）にずらし、±90 度を越えるなら反対側にする。
     * 前の pitch から MIN_MOVE 度以上離れる（取れなければ一番離れたもの）。
     */
    public static double nextVerticalPitch(double lookPitch, double prevPitch, double fovDegrees, Random random) {
        return nextVerticalPitch(lookPitch, prevPitch, 0, fovDegrees, random);
    }

    /** 上（-1）か下（+1）を決めた版（1.8.1。0 ならランダム）。 */
    public static double nextVerticalPitch(double lookPitch, double prevPitch, int pitchSign, double fovDegrees,
                                           Random random) {
        double[] range = offsetRange(fovDegrees);
        double best = lookPitch;
        double bestMove = -1;
        for (int i = 0; i < 40; i++) {
            double offset = range[0] + random.nextDouble() * (range[1] - range[0]);
            double sign = pitchSign != 0 ? pitchSign : random.nextBoolean() ? 1 : -1;
            double pitch = lookPitch + sign * offset;
            if (Math.abs(pitch) > 90) {
                pitch = lookPitch - sign * offset;
            }
            pitch = Math.max(-90, Math.min(90, pitch));
            double move = Math.abs(pitch - prevPitch);
            if (move >= MIN_MOVE_DEGREES) {
                return pitch;
            }
            if (move > bestMove) {
                bestMove = move;
                best = pitch;
            }
        }
        return best;
    }

    /**
     * 照準の左右だけに出す弱点の次の yaw（度。1.8.0。近接の弱点）。pitch は使う側が視線に合わせる。
     * 視線から offsetRange の角度だけ左か右（ランダム）にずらす。前の yaw から MIN_MOVE 度以上離れる（取れなければ一番離れたもの）。
     * 前の yaw との差は、一周（360 度）をまたいでも正しく測る。
     */
    public static double nextHorizontalYaw(double lookYaw, double prevYaw, double fovDegrees, Random random) {
        return nextHorizontalYaw(lookYaw, prevYaw, 0, fovDegrees, random);
    }

    /** 左右を決めた版（1.8.1。yawSign は yaw が増える側 +1 か減る側 -1。0 ならランダム）。 */
    public static double nextHorizontalYaw(double lookYaw, double prevYaw, int yawSign, double fovDegrees,
                                           Random random) {
        double[] range = offsetRange(fovDegrees);
        double best = lookYaw;
        double bestMove = -1;
        for (int i = 0; i < 40; i++) {
            double offset = range[0] + random.nextDouble() * (range[1] - range[0]);
            double yaw = lookYaw + (yawSign != 0 ? yawSign : random.nextBoolean() ? 1 : -1) * offset;
            double move = Math.abs(wrapDegrees(yaw - prevYaw));
            if (move >= MIN_MOVE_DEGREES) {
                return yaw;
            }
            if (move > bestMove) {
                bestMove = move;
                best = yaw;
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

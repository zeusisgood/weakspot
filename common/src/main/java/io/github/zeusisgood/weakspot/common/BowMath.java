package io.github.zeusisgood.weakspot.common;

import java.util.Random;

/** 向きは Minecraft と同じ yaw / pitch（度）。 */
public final class BowMath {

    /** バニラの弓を引き切るまでの tick。 */
    public static final int FULL_DRAW_TICKS = 20;
    /** 視線からずらす角度。近すぎると、照準を動かさずに当たる。 */
    public static final double MIN_OFFSET_DEGREES = 10.0;
    public static final double MAX_OFFSET_DEGREES = 20.0;
    /** ずらす角度の上限を、縦の視野角の半分のこの割合までに抑える（画面からはみ出さないように）。 */
    public static final double MAX_OFFSET_FOV_FRACTION = 0.7;
    public static final double MIN_MOVE_DEGREES = 8.0;
    /** この範囲の外では、次の弱点を水平に戻る側に出す（当て続けても、照準が真上・真下まで行かないように）。 */
    public static final double HORIZON_BAND_DEGREES = 20.0;
    /** 弱点が照準からこれより離れたら（ボートで曲がった、振り向いた）、照準の近くに出し直す。MAX_OFFSET_DEGREES より外。 */
    public static final double RELOCATE_DEGREES = 35.0;
    /** 過剰チャージ（引き切ったあとのヒット）1 回で上がる、矢のダメージの割合。 */
    public static final double OVERCHARGE_PER_HIT = 0.10;
    public static final int MAX_OVERCHARGE_HITS = 5;

    private BowMath() {
    }

    /** 向き（yaw, pitch。度）の単位ベクトル。Minecraft の Entity#getVectorForRotation と同じ向き。 */
    public static double[] vector(double yaw, double pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new double[] {-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    /** vector の逆。{yaw, pitch} を返す。 */
    public static double[] rotation(double[] d) {
        double pitch = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -d[1]))));
        double yaw = Math.toDegrees(Math.atan2(-d[0], d[2]));
        return new double[] {yaw, pitch};
    }

    public static double angleBetween(double yaw1, double pitch1, double yaw2, double pitch2) {
        double[] a = vector(yaw1, pitch1);
        double[] b = vector(yaw2, pitch2);
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
    }

    /** {下限, 上限} を返す。fovDegrees が 0 以下（分からない）なら、視野角で抑えない。 */
    public static double[] offsetRange(double fovDegrees) {
        double max = MAX_OFFSET_DEGREES;
        if (fovDegrees > 0) {
            max = Math.min(max, fovDegrees / 2 * MAX_OFFSET_FOV_FRACTION);
        }
        return new double[] {Math.min(MIN_OFFSET_DEGREES, max / 2), max};
    }

    public static double[] nextSpot(double lookYaw, double lookPitch, double prevYaw, double prevPitch,
                                    Random random) {
        return nextSpot(lookYaw, lookPitch, prevYaw, prevPitch, 0, random);
    }

    /**
     * {yaw, pitch} を返す。最初の弱点は prev に視線を渡す。yaw は、前の向きとの差が ±180 度に収まる形で返す
     * （表示の移動が遠回りしないように）。
     */
    public static double[] nextSpot(double lookYaw, double lookPitch, double prevYaw, double prevPitch,
                                    double fovDegrees, Random random) {
        return nextSpot(lookYaw, lookPitch, prevYaw, prevPitch, 0, 0, fovDegrees, random);
    }

    /** yawSign は視線より yaw が増える側（+1）か減る側（-1）、pitchSign は下（+1）か上（-1）。0 ならどちらでもよい。 */
    public static double[] nextSpot(double lookYaw, double lookPitch, double prevYaw, double prevPitch,
                                    int yawSign, int pitchSign, double fovDegrees, Random random) {
        double[] range = offsetRange(fovDegrees);
        double[] look = vector(lookYaw, lookPitch);
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

    /** 上下だけの弱点は usePitch だけ、左右だけの弱点は useYaw だけ、全方向の弱点は両方を true にする。 */
    public static boolean isTooFar(double spotYaw, double spotPitch, double lookYaw, double lookPitch,
                                   boolean useYaw, boolean usePitch) {
        double distance;
        if (useYaw && usePitch) {
            distance = angleBetween(spotYaw, spotPitch, lookYaw, lookPitch);
        } else if (useYaw) {
            distance = Math.abs(wrapDegrees(spotYaw - lookYaw));
        } else {
            distance = Math.abs(spotPitch - lookPitch);
        }
        return distance > RELOCATE_DEGREES;
    }

    private static boolean onSide(double delta, int sign) {
        return sign == 0 || delta * sign >= 0;
    }

    /** 上（-1）か下（+1）を返す。最初は prevSign に 0 を渡す。 */
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

    public static int nextYawSign(int prevSign, Random random) {
        return alternate(prevSign, random);
    }

    private static int alternate(int prevSign, Random random) {
        if (prevSign == 0) {
            return random.nextBoolean() ? 1 : -1;
        }
        return -Integer.signum(prevSign);
    }

    /** 照準の真上か真下だけに出す弱点の pitch。yaw は使う側が視線に合わせる。 */
    public static double nextVerticalPitch(double lookPitch, double prevPitch, double fovDegrees, Random random) {
        return nextVerticalPitch(lookPitch, prevPitch, 0, fovDegrees, random);
    }

    /** pitchSign は上（-1）か下（+1）。0 ならランダム。 */
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

    /** 照準の左右だけに出す弱点の yaw。pitch は使う側が視線に合わせる。yawSign は増える側（+1）か減る側（-1）。0 ならランダム。 */
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

    /**
     * 左右だけの弱点（近接）の pitch が視線についてくる範囲（度）。真上・真下では yaw をずらしても同じ点に
     * なり、弱点が照準の中央に出てしまうので、これより深く向いても弱点の pitch はここで止める。
     */
    public static final double HORIZONTAL_PITCH_LIMIT = 60.0;

    public static double horizontalPitch(double lookPitch) {
        return Math.max(-HORIZONTAL_PITCH_LIMIT, Math.min(HORIZONTAL_PITCH_LIMIT, lookPitch));
    }

    /**
     * 左右だけの弱点の yaw のずれを広げる倍率。上下を向くほど画面の上で左右の幅が cos(pitch) 倍に縮むので、
     * 1 / cos(horizontalPitch) 倍に広げて、画面の上で照準から 10〜20 度離れたままにする（60 度で 2 倍）。
     */
    public static double horizontalYawScale(double lookPitch) {
        return 1 / Math.cos(Math.toRadians(horizontalPitch(lookPitch)));
    }

    public static double nextWideHorizontalYaw(double lookYaw, double lookPitch, double prevYaw, int yawSign,
                                               double fovDegrees, Random random) {
        double yaw = nextHorizontalYaw(lookYaw, prevYaw, yawSign, fovDegrees, random);
        return lookYaw + wrapDegrees(yaw - lookYaw) * horizontalYawScale(lookPitch);
    }

    /** 広げた分を戻して、画面の上の角度で測る。 */
    public static boolean isTooFarHorizontal(double spotYaw, double lookYaw, double lookPitch) {
        return Math.abs(wrapDegrees(spotYaw - lookYaw)) / horizontalYawScale(lookPitch) > RELOCATE_DEGREES;
    }

    public static int addedTicks(int used, int hitTicks) {
        return addedTicks(used, hitTicks, FULL_DRAW_TICKS);
    }

    /** fullTicks は、その物を引き切る（装填し切る）までの tick。 */
    public static int addedTicks(int used, int hitTicks, int fullTicks) {
        return Math.max(0, Math.min(hitTicks, fullTicks - used));
    }

    /**
     * 弓の 1 ヒット分（hitTicks）を、引き切りが fullTicks の物（トライデント・クロスボウ）に割合で直した tick。
     * carry は前のヒットまでの端数（1 / FULL_DRAW_TICKS tick 単位）で、次の carry は nextScaledCarry。
     */
    public static int scaledHitTicks(int hitTicks, int fullTicks, int carry) {
        return (hitTicks * fullTicks + carry) / FULL_DRAW_TICKS;
    }

    public static int nextScaledCarry(int hitTicks, int fullTicks, int carry) {
        return (hitTicks * fullTicks + carry) % FULL_DRAW_TICKS;
    }

    public static double overchargeMultiplier(int hits) {
        return 1.0 + OVERCHARGE_PER_HIT * Math.max(0, Math.min(hits, MAX_OVERCHARGE_HITS));
    }

    public static boolean canOvercharge(int hits) {
        return hits < MAX_OVERCHARGE_HITS;
    }

    public static boolean isFull(int used) {
        return used >= FULL_DRAW_TICKS;
    }

    /** 0〜1 を返す。used は tick（フレームの途中の値でもよい）。 */
    public static double barValue(double used) {
        return barValue(used, FULL_DRAW_TICKS);
    }

    public static double barValue(double used, int fullTicks) {
        return fullTicks <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, used / fullTicks));
    }

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

package com.example.weakspot.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 弱点の大きさと出現位置を決める。 */
public final class WeakSpotPlacer {

    /** 位置候補のグリッドの分割数。条件を満たす候補がなければ、最も遠い候補を使う。 */
    private static final int GRID = 6;

    private WeakSpotPlacer() {
    }

    /** 弱点の半径。細長い面（ハーフブロックの側面など）では短い辺を基準にする。 */
    public static double radius(FaceRect face, double radiusRatio) {
        return Math.min(face.width(), face.height()) * radiusRatio;
    }

    /**
     * 弱点の中心位置 (u, v) を選ぶ。
     * 円が面からはみ出さず縁から少し離れる範囲で、(avoidU, avoidV) から minDistance 以上離れた位置を選ぶ。
     */
    public static double[] place(FaceRect face, double radius, double avoidU, double avoidV,
                                 double minDistance, Random random) {
        double margin = radius * 1.35;
        double loU = face.minU + margin;
        double hiU = face.maxU - margin;
        double loV = face.minV + margin;
        double hiV = face.maxV - margin;
        if (loU > hiU) {
            loU = hiU = face.centerU();
        }
        if (loV > hiV) {
            loV = hiV = face.centerV();
        }

        // 面をグリッドに分けて各セルから1点ずつ候補を取り、条件を満たす候補からランダムに選ぶ。
        // 中央付近から遠ざけると角しか残らないことがあるので、範囲の四隅も候補に入れる。
        List<double[]> valid = new ArrayList<>();
        double[] best = null;
        double bestDistance = -1;
        for (int i = 0; i < GRID * GRID + 4; i++) {
            double u;
            double v;
            if (i < GRID * GRID) {
                u = loU + (hiU - loU) * ((i % GRID) + random.nextDouble()) / GRID;
                v = loV + (hiV - loV) * ((i / GRID) + random.nextDouble()) / GRID;
            } else {
                int corner = i - GRID * GRID;
                u = (corner & 1) == 0 ? loU : hiU;
                v = (corner & 2) == 0 ? loV : hiV;
            }
            double d = FaceMath.distance(u, v, avoidU, avoidV);
            double[] candidate = {u, v};
            if (d >= minDistance) {
                valid.add(candidate);
            }
            if (d > bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return valid.isEmpty() ? best : valid.get(random.nextInt(valid.size()));
    }

    public static boolean isHit(double spotU, double spotV, double radius, double aimU, double aimV) {
        return FaceMath.distance(spotU, spotV, aimU, aimV) <= radius;
    }
}

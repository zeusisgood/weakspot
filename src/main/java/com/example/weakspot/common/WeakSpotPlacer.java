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

    /** 縁の余白の上限（面の短い辺に対する比率）。小さい面で、中心の動ける範囲が消えないようにする。 */
    static final double EDGE_MARGIN_FACE_RATIO = 0.12;
    /** 最小移動距離の上限（動ける範囲の対角線に対する比率）。 */
    static final double MOVE_DISTANCE_RANGE_RATIO = 0.6;

    /** 面に合わせて決めた、弱点の大きさ・縁の余白・最小移動距離。 */
    public static final class Layout {
        public final double radius;
        public final double edgeMargin;
        public final double minMoveDistance;

        Layout(double radius, double edgeMargin, double minMoveDistance) {
            this.radius = radius;
            this.edgeMargin = edgeMargin;
            this.minMoveDistance = minMoveDistance;
        }
    }

    /**
     * 面に合わせた配置の数値（SPEC_v1.2 §1.3）。ふつうの1×1の面では、設定値のまま（半径 = 短い辺 × radiusRatio）。
     * 1. 半径は minRadius 以上、短い辺 × maxRadiusRatio 以下（両立しないときは上限を優先する）
     * 2. 縁の余白は min(edgeMargin, 短い辺 × 0.12)
     * 3. 最小移動距離は min(minMoveDistance, 動ける範囲の対角線 × 0.6)
     */
    public static Layout layout(FaceRect face, double radiusRatio, double minRadius, double maxRadiusRatio,
                                double edgeMargin, double minMoveDistance) {
        double shorter = Math.min(face.width(), face.height());
        double radius = Math.min(Math.max(shorter * radiusRatio, minRadius), shorter * maxRadiusRatio);
        double margin = Math.min(Math.max(0, edgeMargin), shorter * EDGE_MARGIN_FACE_RATIO);
        double rangeU = Math.max(0, face.width() - 2 * (radius + margin));
        double rangeV = Math.max(0, face.height() - 2 * (radius + margin));
        double move = Math.min(minMoveDistance, Math.sqrt(rangeU * rangeU + rangeV * rangeV) * MOVE_DISTANCE_RANGE_RATIO);
        return new Layout(radius, margin, move);
    }

    /** 面の短い辺が minFaceSize より小さければ、弱点を出さない（カーペットの側面など）。 */
    public static boolean isTooSmall(FaceRect face, double minFaceSize) {
        return Math.min(face.width(), face.height()) < minFaceSize;
    }

    /**
     * 弱点の中心位置 (u, v) を選ぶ。
     * 円の外周と面の縁の間を edgeMargin 以上空けた範囲で、(avoidU, avoidV) から minDistance 以上離れた位置を選ぶ。
     * 面が小さくてその範囲が取れない軸では、面の中央に置く。
     */
    public static double[] place(FaceRect face, double radius, double edgeMargin, double avoidU, double avoidV,
                                 double minDistance, Random random) {
        double margin = radius + Math.max(0, edgeMargin);
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

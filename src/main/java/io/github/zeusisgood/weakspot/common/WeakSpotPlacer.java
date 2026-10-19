package io.github.zeusisgood.weakspot.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WeakSpotPlacer {

    /** 条件を満たす候補がなければ、最も遠い候補を使う。 */
    private static final int GRID = 6;

    private WeakSpotPlacer() {
    }

    /** 細長い面（ハーフブロックの側面など）では短い辺を基準にする。 */
    public static double radius(FaceRect face, double radiusRatio) {
        return Math.min(face.width(), face.height()) * radiusRatio;
    }

    /** 面の短い辺に対する比率。小さい面で、中心の動ける範囲が消えないようにする。 */
    static final double EDGE_MARGIN_FACE_RATIO = 0.12;
    /** 動ける範囲の対角線に対する比率。 */
    static final double MOVE_DISTANCE_RANGE_RATIO = 0.6;

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

    /** ふつうの 1×1 の面では、設定値のまま。半径の下限と上限が両立しないときは上限を優先する。 */
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

    /** カーペットの側面など。 */
    public static boolean isTooSmall(FaceRect face, double minFaceSize) {
        return Math.min(face.width(), face.height()) < minFaceSize;
    }

    /** 面が小さくて余白と移動距離を取れない軸では、面の中央に置く。 */
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

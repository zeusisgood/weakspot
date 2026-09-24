package com.example.weakspot.common;

/**
 * ブロック面のローカル2D座標 (u, v) とワールド座標の相互変換。
 * Minecraft のクラスに依存しない。軸は 0=X, 1=Y, 2=Z で表す。
 *
 * 法線軸ごとの (u, v) の割り当て:
 * X面 → (z, y) / Y面 → (x, z) / Z面 → (x, y)
 * 距離の比較にしか使わないので、向き（左右反転など）は気にしない。
 */
public final class FaceMath {

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_Z = 2;

    private FaceMath() {
    }

    public static int uAxis(int normalAxis) {
        return normalAxis == AXIS_X ? AXIS_Z : AXIS_X;
    }

    public static int vAxis(int normalAxis) {
        return normalAxis == AXIS_Y ? AXIS_Z : AXIS_Y;
    }

    /** ワールド座標 (x, y, z) を面の (u, v) に変換する。 */
    public static double[] toFaceUV(int normalAxis, double x, double y, double z) {
        double[] p = {x, y, z};
        return new double[] {p[uAxis(normalAxis)], p[vAxis(normalAxis)]};
    }

    /** 面の (u, v) と、法線軸方向の座標 plane からワールド座標を作る。 */
    public static double[] toWorld(int normalAxis, double plane, double u, double v) {
        double[] p = new double[3];
        p[normalAxis] = plane;
        p[uAxis(normalAxis)] = u;
        p[vAxis(normalAxis)] = v;
        return p;
    }

    /** ブロックの AABB から、その面の (u, v) の矩形を作る。 */
    public static FaceRect faceRect(int normalAxis, double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        double[] min = {minX, minY, minZ};
        double[] max = {maxX, maxY, maxZ};
        int u = uAxis(normalAxis);
        int v = vAxis(normalAxis);
        return new FaceRect(min[u], min[v], max[u], max[v]);
    }

    /**
     * 大きさ dx × dy × dz の箱で、面積が一番大きい面の法線軸。
     * 同じ大きさなら Y（上面）、次に X、Z の順に選ぶ（作物・苗木の弱点は上面に出したいため）。
     */
    public static int largestFaceAxis(double dx, double dy, double dz) {
        double eps = 1e-6;
        double x = dy * dz;
        double y = dx * dz;
        double z = dx * dy;
        if (y + eps >= x && y + eps >= z) {
            return AXIS_Y;
        }
        return x + eps >= z ? AXIS_X : AXIS_Z;
    }

    public static double distance(double u1, double v1, double u2, double v2) {
        double du = u1 - u2;
        double dv = v1 - v2;
        return Math.sqrt(du * du + dv * dv);
    }
}

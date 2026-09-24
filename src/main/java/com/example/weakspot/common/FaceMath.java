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

    /** 大きさ dx × dy × dz の箱で、法線軸 axis の面が一番大きい面（同じ大きさを含む）か。 */
    public static boolean isLargestFace(int axis, double dx, double dy, double dz) {
        double[] area = {dy * dz, dx * dz, dx * dy};
        double max = Math.max(area[AXIS_X], Math.max(area[AXIS_Y], area[AXIS_Z]));
        return area[axis] + 1e-6 >= max;
    }

    /**
     * 成長の弱点を出す面の法線軸。上面が一番大きい（同じ大きさを含む）なら、常に Y（作物・苗木）。
     * 側面が一番大きいときは、一番大きい側面のうち、次の順に選ぶ。
     * 1. keepAxis: 今弱点が出ていて、プレイヤーから見えている面の軸（照準が別の側面へ移っても、面を変えない）
     * 2. aimedAxis: 照準が当たっている面の軸
     * 3. 視点の、箱の中心からのずれ (eyeOffsetX, eyeOffsetZ) が大きいほうの軸（同じなら X）
     * 使わない軸は -1 を渡す。
     */
    public static int growthFaceAxis(double dx, double dy, double dz, int keepAxis, int aimedAxis,
                                     double eyeOffsetX, double eyeOffsetZ) {
        int largest = largestFaceAxis(dx, dy, dz);
        if (largest == AXIS_Y) {
            return AXIS_Y;
        }
        if (isLargestSide(keepAxis, dx, dy, dz)) {
            return keepAxis;
        }
        if (isLargestSide(aimedAxis, dx, dy, dz)) {
            return aimedAxis;
        }
        if (isLargestFace(AXIS_X, dx, dy, dz) && isLargestFace(AXIS_Z, dx, dy, dz)) {
            return Math.abs(eyeOffsetZ) > Math.abs(eyeOffsetX) ? AXIS_Z : AXIS_X;
        }
        return largest;
    }

    private static boolean isLargestSide(int axis, double dx, double dy, double dz) {
        return (axis == AXIS_X || axis == AXIS_Z) && isLargestFace(axis, dx, dy, dz);
    }

    public static double distance(double u1, double v1, double u2, double v2) {
        double du = u1 - u2;
        double dv = v1 - v2;
        return Math.sqrt(du * du + dv * dv);
    }
}

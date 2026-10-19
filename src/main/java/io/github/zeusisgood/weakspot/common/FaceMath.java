package io.github.zeusisgood.weakspot.common;

/**
 * ブロック面のローカル 2D 座標 (u, v) とワールド座標の変換。軸は 0=X, 1=Y, 2=Z。
 * (u, v) は X 面 → (z, y)、Y 面 → (x, z)、Z 面 → (x, y)。距離の比較にしか使わないので、向き（左右反転など）は気にしない。
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

    public static double[] toFaceUV(int normalAxis, double x, double y, double z) {
        double[] p = {x, y, z};
        return new double[] {p[uAxis(normalAxis)], p[vAxis(normalAxis)]};
    }

    /** plane は法線軸方向の座標。 */
    public static double[] toWorld(int normalAxis, double plane, double u, double v) {
        double[] p = new double[3];
        p[normalAxis] = plane;
        p[uAxis(normalAxis)] = u;
        p[vAxis(normalAxis)] = v;
        return p;
    }

    public static FaceRect faceRect(int normalAxis, double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        double[] min = {minX, minY, minZ};
        double[] max = {maxX, maxY, maxZ};
        int u = uAxis(normalAxis);
        int v = vAxis(normalAxis);
        return new FaceRect(min[u], min[v], max[u], max[v]);
    }

    /** 同じ大きさなら Y、X、Z の順（作物・苗木の弱点は上面に出したいため）。 */
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

    /** 同じ大きさを含む。 */
    public static boolean isLargestFace(int axis, double dx, double dy, double dz) {
        double[] area = {dy * dz, dx * dz, dx * dy};
        double max = Math.max(area[AXIS_X], Math.max(area[AXIS_Y], area[AXIS_Z]));
        return area[axis] + 1e-6 >= max;
    }

    /**
    * 上面が一番大きければ（同じ大きさを含む）Y。側面が一番大きいときは、一番大きい側面のうち、
    * 1. keepAxis（今弱点が出ていて見えている面。照準が別の側面へ移っても、面を変えない）
    * 2. aimedAxis（照準が当たっている面）
    * 3. 視点の、箱の中心からのずれが大きいほうの軸（同じなら X）
    * の順に選ぶ。使わない軸は -1 を渡す。
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

    /** p は箱の中か表面の点。{法線軸, 向き(+1 / -1)} を返す。同じ距離なら Y、X、Z の順。 */
    public static int[] nearestFace(double[] p, double[] min, double[] max) {
        int bestAxis = AXIS_Y;
        int bestSign = 1;
        double best = Double.MAX_VALUE;
        int[] order = {AXIS_Y, AXIS_X, AXIS_Z};
        for (int axis : order) {
            double toMax = Math.abs(max[axis] - p[axis]);
            double toMin = Math.abs(p[axis] - min[axis]);
            if (toMax < best - 1e-9) {
                best = toMax;
                bestAxis = axis;
                bestSign = 1;
            }
            if (toMin < best - 1e-9) {
                best = toMin;
                bestAxis = axis;
                bestSign = -1;
            }
        }
        return new int[] {bestAxis, bestSign};
    }

    public static double distance(double u1, double v1, double u2, double v2) {
        double du = u1 - u2;
        double dv = v1 - v2;
        return Math.sqrt(du * du + dv * dv);
    }
}

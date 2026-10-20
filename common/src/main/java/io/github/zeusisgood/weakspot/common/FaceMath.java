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

    /**
     * 目 eye から向き dir へ伸ばした線が、箱に外から入る面。{法線軸, 向き(+1 / -1)} を返す。
     * 目が箱の中か面の上にある・線が箱に届かないときは null（呼ぶ側は、バニラの照準の面と点に戻す）。
     */
    public static int[] rayEntryFace(double[] eye, double[] dir, double[] min, double[] max) {
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        int enterAxis = -1;
        int enterSign = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(dir[axis]) < 1e-12) {
                if (eye[axis] < min[axis] || eye[axis] > max[axis]) {
                    return null;
                }
                continue;
            }
            double t1 = (min[axis] - eye[axis]) / dir[axis];
            double t2 = (max[axis] - eye[axis]) / dir[axis];
            double near = Math.min(t1, t2);
            double far = Math.max(t1, t2);
            if (near > enter) {
                enter = near;
                enterAxis = axis;
                // 正の向きに進む線は、負の側の面から入る
                enterSign = dir[axis] > 0 ? -1 : 1;
            }
            exit = Math.min(exit, far);
        }
        if (enterAxis < 0 || enter <= 0 || enter > exit) {
            return null;
        }
        return new int[] {enterAxis, enterSign};
    }

    /** 線と、法線軸 normalAxis の座標が plane の平面の交点。目の後ろ・平面と平行なら null。 */
    public static double[] rayPlane(double[] eye, double[] dir, int normalAxis, double plane) {
        if (Math.abs(dir[normalAxis]) < 1e-12) {
            return null;
        }
        double t = (plane - eye[normalAxis]) / dir[normalAxis];
        if (t <= 0) {
            return null;
        }
        return new double[] {eye[0] + dir[0] * t, eye[1] + dir[1] * t, eye[2] + dir[2] * t};
    }

    public static double distance(double u1, double v1, double u2, double v2) {
        double du = u1 - u2;
        double dv = v1 - v2;
        return Math.sqrt(du * du + dv * dv);
    }
}

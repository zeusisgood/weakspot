package io.github.zeusisgood.weakspot.common;

/**
 * 掘っているブロックの残りの耐久バー。面の「下」の辺に沿って左から伸び、減ると右端から縮む。
 * 座標は面の (u, v)（FaceMath の割り当て）で、ワールド座標のまま。
 * 「下」の辺は、側面なら下の辺、上面・下面ならプレイヤーに一番近い辺。左右は、その辺から面の奥を向いたときの右手の側を右にする。
 */
public final class BlockHealthBar {

    /** ブロック。 */
    public static final double THICKNESS = 0.06;
    /** 下の辺からバーの中心まで（ブロック）。 */
    public static final double INSET = 0.05;
    public static final double LENGTH_RATIO = 0.8;

    /** false なら v の向き。 */
    public final boolean alongU;
    /** 伸びる向きの座標。右が小さい側のこともある。 */
    public final double left;
    public final double right;
    /** 伸びる向きと直交する座標。 */
    public final double across;

    private BlockHealthBar(boolean alongU, double left, double right, double across) {
        this.alongU = alongU;
        this.left = left;
        this.right = right;
        this.across = across;
    }

    /** 0〜1 を返す。 */
    public static double remaining(double progress) {
        return Math.max(0, Math.min(1, 1 - progress));
    }

    /** partial は 0〜1。 */
    public static double interpolate(double prev, double current, double partial) {
        return prev + (current - prev) * partial;
    }

    /** normalSign は +1 / -1。rect は実際の当たり判定の箱の面。eyeX, eyeZ は上面・下面で使う。 */
    public static BlockHealthBar place(int normalAxis, int normalSign, FaceRect rect, double eyeX, double eyeZ) {
        if (normalAxis != FaceMath.AXIS_Y) {
            // 側面: v は Y なので、下の辺は minV。u は、X面なら Z、Z面なら X
            // プレイヤーは面の外向きの法線 n の逆を向き、右手は (n_z, 0, -n_x)
            int rightSign = normalAxis == FaceMath.AXIS_X ? -normalSign : normalSign;
            return along(true, rect.minU, rect.maxU, rightSign, rect.minV + INSET);
        }
        // 上面・下面: (u, v) = (x, z)。視点が一番外側にはみ出している辺を選ぶ（同じなら -X, +X, -Z, +Z の順）
        double[] out = {rect.minU - eyeX, eyeX - rect.maxU, rect.minV - eyeZ, eyeZ - rect.maxV};
        int edge = 0;
        for (int i = 1; i < out.length; i++) {
            if (out[i] > out[edge] + 1e-9) {
                edge = i;
            }
        }
        // 辺の外向きの法線 e（水平）。プレイヤーは -e を向き、右手は (e_z, 0, -e_x)
        if (edge < 2) {
            int ex = edge == 0 ? -1 : 1;
            double x = edge == 0 ? rect.minU + INSET : rect.maxU - INSET;
            return along(false, rect.minV, rect.maxV, -ex, x);
        }
        int ez = edge == 2 ? -1 : 1;
        double z = edge == 2 ? rect.minV + INSET : rect.maxV - INSET;
        return along(true, rect.minU, rect.maxU, ez, z);
    }

    /** rightSign は、右手が座標の正の向きなら +1。 */
    private static BlockHealthBar along(boolean alongU, double min, double max, int rightSign, double across) {
        double center = (min + max) / 2;
        double half = (max - min) * LENGTH_RATIO / 2;
        return rightSign > 0
                ? new BlockHealthBar(alongU, center - half, center + half, across)
                : new BlockHealthBar(alongU, center + half, center - half, across);
    }

    /** {minU, minV, maxU, maxV} を返す。fraction = 1 で背景（バーの全体）。 */
    public double[] rect(double fraction) {
        double end = left + (right - left) * fraction;
        double a0 = Math.min(left, end);
        double a1 = Math.max(left, end);
        double c0 = across - THICKNESS / 2;
        double c1 = across + THICKNESS / 2;
        return alongU ? new double[] {a0, c0, a1, c1} : new double[] {c0, a0, c1, a1};
    }
}

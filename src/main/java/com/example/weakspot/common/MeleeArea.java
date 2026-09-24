package com.example.weakspot.common;

/**
 * 近接の弱点を出す範囲（SPEC_v1.3.1）。Minecraft に依存しない。
 * 足先は狙いにくいので、敵の当たり判定の箱の上のほう（頭・首・胴体のあたり）にだけ出す。
 * 側面は高さの下 EXCLUDED_BOTTOM_RATIO を除き、上面は全体、底面には出さない。
 */
public final class MeleeArea {

    /** 側面で除く、下の割合（バニラのゾンビ・スケルトンの脚は高さの約 37%）。 */
    public static final double EXCLUDED_BOTTOM_RATIO = 0.4;

    private MeleeArea() {
    }

    /**
     * 大きさ width × height の面（(0, 0) が左下の角。側面の v は高さ）のうち、弱点を出す範囲。出さない面は null。
     * normalAxis は面の法線軸（FaceMath の AXIS_*）、positive は法線が正の向きか（上面なら true）。
     */
    public static FaceRect area(double width, double height, int normalAxis, boolean positive) {
        if (normalAxis == FaceMath.AXIS_Y) {
            return positive ? new FaceRect(0, 0, width, height) : null;
        }
        return new FaceRect(0, height * EXCLUDED_BOTTOM_RATIO, width, height);
    }
}

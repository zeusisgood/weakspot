package com.example.weakspot.common;

/** ブロック面の (u, v) 上の矩形。単位はブロック。 */
public final class FaceRect {

    public final double minU;
    public final double minV;
    public final double maxU;
    public final double maxV;

    public FaceRect(double minU, double minV, double maxU, double maxV) {
        this.minU = minU;
        this.minV = minV;
        this.maxU = maxU;
        this.maxV = maxV;
    }

    public double width() {
        return maxU - minU;
    }

    public double height() {
        return maxV - minV;
    }

    public double centerU() {
        return (minU + maxU) / 2;
    }

    public double centerV() {
        return (minV + maxV) / 2;
    }
}

package com.example.weakspot.common;

/**
 * 寝ている間の弱点のマーカーを出す範囲（1.6.2）。画面の中央の、幅・高さとも AREA_FRACTION の範囲。
 * 下のボタンとチャット欄（下から BOTTOM_AVOID）にはかからない。座標は GUI のピクセル。
 */
public final class SleepSpotArea {

    /** 範囲の大きさ（画面の幅・高さに対する割合）。 */
    public static final double AREA_FRACTION = 0.4;
    /** 避ける下の範囲（画面の高さに対する割合）。 */
    public static final double BOTTOM_AVOID = 0.25;
    /** 次のマーカーを離す距離の、マーカーの半径に対する倍率と、画面の短い辺に対する割合。 */
    public static final double MIN_MOVE_RADII = 2.5;
    public static final double MIN_MOVE_SCREEN = 0.15;

    private SleepSpotArea() {
    }

    /** 範囲 {minX, maxX, minY, maxY}。 */
    public static double[] area(double width, double height, double radius) {
        double margin = (1 - AREA_FRACTION) / 2;
        double minX = width * margin;
        double maxX = width * (1 - margin);
        double minY = height * margin;
        double maxY = Math.max(minY, Math.min(height * (1 - margin), height * (1 - BOTTOM_AVOID) - radius));
        return new double[] {minX, maxX, minY, maxY};
    }

    /** 次のマーカーを前の位置から離す距離。 */
    public static double minMove(double width, double height, double radius) {
        return Math.max(radius * MIN_MOVE_RADII, Math.min(width, height) * MIN_MOVE_SCREEN);
    }
}

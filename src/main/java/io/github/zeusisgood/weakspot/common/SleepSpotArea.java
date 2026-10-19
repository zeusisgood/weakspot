package io.github.zeusisgood.weakspot.common;

/** 下のボタンとチャット欄にはかからない。座標は GUI のピクセル。 */
public final class SleepSpotArea {

    /** 画面の幅・高さに対する割合。 */
    public static final double AREA_FRACTION = 0.4;
    /** 画面の高さに対する割合。 */
    public static final double BOTTOM_AVOID = 0.25;
    /** 前のマーカーから離す距離の、半径に対する倍率と、画面の短い辺に対する割合。 */
    public static final double MIN_MOVE_RADII = 2.5;
    public static final double MIN_MOVE_SCREEN = 0.15;

    private SleepSpotArea() {
    }

    /** {minX, maxX, minY, maxY} を返す。 */
    public static double[] area(double width, double height, double radius) {
        double margin = (1 - AREA_FRACTION) / 2;
        double minX = width * margin;
        double maxX = width * (1 - margin);
        double minY = height * margin;
        double maxY = Math.max(minY, Math.min(height * (1 - margin), height * (1 - BOTTOM_AVOID) - radius));
        return new double[] {minX, maxX, minY, maxY};
    }

    public static double minMove(double width, double height, double radius) {
        return Math.max(radius * MIN_MOVE_RADII, Math.min(width, height) * MIN_MOVE_SCREEN);
    }
}

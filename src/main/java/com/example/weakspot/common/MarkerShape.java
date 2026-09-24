package com.example.weakspot.common;

/**
 * 弱点マークの形。大きさは弱点の半径に合わせる（ひし形は頂点が半径の位置、四角は辺が半径の位置の少し内側）。
 * 自分のマークは CIRCLE 固定で、他のプレイヤーのマークだけ設定で選べる。Minecraft に依存しない。
 */
public enum MarkerShape {
    /** 塗りつぶした円。 */
    CIRCLE(32, 0, 1.0, true),
    /** 中抜きの輪。 */
    RING(32, 0, 1.0, false),
    /** ひし形。 */
    DIAMOND(4, 0, 1.0, true),
    /** 四角（辺は u, v の軸に平行）。 */
    SQUARE(4, Math.PI / 4, 0.8 * Math.sqrt(2), true);

    /** 輪の内側の半径の比率（RING の太さ）。 */
    public static final double RING_INNER_RATIO = 0.7;

    /** 輪郭の頂点の数。 */
    public final int sides;
    /** 最初の頂点の角度（ラジアン）。 */
    public final double rotation;
    /** 弱点の半径に掛けて、頂点までの距離にする。 */
    public final double radiusScale;
    /** 中を塗るか（RING は輪の部分だけ塗る）。 */
    public final boolean filled;

    MarkerShape(int sides, double rotation, double radiusScale, boolean filled) {
        this.sides = sides;
        this.rotation = rotation;
        this.radiusScale = radiusScale;
        this.filled = filled;
    }

    /** 中心に小さな点を描く形か（円、ひし形、四角。RING は中抜きなので描かない）。 */
    public boolean hasCenterDot() {
        return filled;
    }

    /** 設定の値から形を選ぶ。null や知らない値のときは RING。 */
    public static MarkerShape fromName(String name) {
        if (name != null) {
            for (MarkerShape shape : values()) {
                if (shape.name().equalsIgnoreCase(name.trim())) {
                    return shape;
                }
            }
        }
        return RING;
    }
}

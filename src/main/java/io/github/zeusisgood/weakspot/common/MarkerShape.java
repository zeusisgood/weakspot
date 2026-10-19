package io.github.zeusisgood.weakspot.common;

/** 自分のマークは CIRCLE 固定。他のプレイヤーのマークだけ設定で選べる。 */
public enum MarkerShape {
    CIRCLE(32, 0, 1.0, true),
    RING(32, 0, 1.0, false),
    DIAMOND(4, 0, 1.0, true),
    /** 辺は u, v の軸に平行。 */
    SQUARE(4, Math.PI / 4, 0.8 * Math.sqrt(2), true),
    /** 的当ての銅のご褒美。 */
    STAR(10, -Math.PI / 2, 1.2, true);

    public static final double STAR_INNER_RATIO = 0.45;

    /** RING の太さ。 */
    public static final double RING_INNER_RATIO = 0.7;

    public final int sides;
    /** ラジアン。 */
    public final double rotation;
    /** 弱点の半径に掛けて、頂点までの距離にする。 */
    public final double radiusScale;
    /** RING は輪の部分だけ塗る。 */
    public final boolean filled;

    MarkerShape(int sides, double rotation, double radiusScale, boolean filled) {
        this.sides = sides;
        this.rotation = rotation;
        this.radiusScale = radiusScale;
        this.filled = filled;
    }

    public double pointScale(int i) {
        return this == STAR && i % 2 == 1 ? STAR_INNER_RATIO : 1.0;
    }

    /** RING は中抜きなので描かない。 */
    public boolean hasCenterDot() {
        return filled;
    }

    /** null や知らない値のときは RING。 */
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

package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarkerShapeTest {

    @Test
    public void picksShapeFromConfigValue() {
        assertEquals(MarkerShape.CIRCLE, MarkerShape.fromName("CIRCLE"));
        assertEquals(MarkerShape.DIAMOND, MarkerShape.fromName("diamond"));
        assertEquals(MarkerShape.SQUARE, MarkerShape.fromName(" SQUARE "));
        assertEquals(MarkerShape.RING, MarkerShape.fromName("RING"));
    }

    @Test
    public void unknownValueFallsBackToRing() {
        assertEquals(MarkerShape.STAR, MarkerShape.fromName("star"));
        assertEquals(MarkerShape.RING, MarkerShape.fromName("HEXAGON"));
        assertEquals(1.0, MarkerShape.STAR.pointScale(0), 0);
        assertEquals(MarkerShape.STAR_INNER_RATIO, MarkerShape.STAR.pointScale(1), 0);
        assertEquals(1.0, MarkerShape.DIAMOND.pointScale(1), 0);
        assertEquals(MarkerShape.RING, MarkerShape.fromName(""));
        assertEquals(MarkerShape.RING, MarkerShape.fromName(null));
    }

    @Test
    public void ringIsHollow() {
        assertFalse(MarkerShape.RING.filled);
        assertFalse(MarkerShape.RING.hasCenterDot());
        assertTrue(MarkerShape.CIRCLE.filled);
        assertTrue(MarkerShape.CIRCLE.hasCenterDot());
    }

    @Test
    public void squareSidesAreAxisAligned() {
        // 頂点が 45° から始まるので、辺は u, v の軸に平行。辺の半分の長さは半径の 0.8 倍
        double half = MarkerShape.SQUARE.radiusScale * Math.cos(MarkerShape.SQUARE.rotation);
        assertEquals(0.8, half, 1e-9);
    }
}

package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public class WeakSpotPlacerTest {

    private static final FaceRect FULL = new FaceRect(0, 0, 1, 1);

    @Test
    public void radiusUsesShorterSide() {
        assertEquals(0.14, WeakSpotPlacer.radius(FULL, 0.14), 1e-9);
        assertEquals(0.07, WeakSpotPlacer.radius(new FaceRect(0, 0, 1, 0.5), 0.14), 1e-9);
    }

    private static final double RATIO = 0.14;
    private static final double MIN_RADIUS = 0.08;
    private static final double MAX_RATIO = 0.35;
    private static final double EDGE = 0.1;
    private static final double MOVE = 0.4;

    private static WeakSpotPlacer.Layout layout(FaceRect face) {
        return WeakSpotPlacer.layout(face, RATIO, MIN_RADIUS, MAX_RATIO, EDGE, MOVE);
    }

    @Test
    public void fullBlockLayoutIsUnchanged() {
        WeakSpotPlacer.Layout l = layout(FULL);
        assertEquals(0.14, l.radius, 1e-9);
        assertEquals(0.1, l.edgeMargin, 1e-9);
        assertEquals(0.4, l.minMoveDistance, 1e-9);
    }

    @Test
    public void halfBlockSideUsesMinRadiusAndKeepsMoveDistance() {
        FaceRect side = new FaceRect(0, 0, 1, 0.5);
        WeakSpotPlacer.Layout l = layout(side);
        assertEquals(0.08, l.radius, 1e-9);
        assertEquals(0.06, l.edgeMargin, 1e-9);
        assertEquals(0.4, l.minMoveDistance, 1e-9);
        assertMovesFarEnough(side, l);
    }

    @Test
    public void buttonFrontPlacesAndMovesInsideItsRange() {
        FaceRect button = new FaceRect(0, 0, 0.375, 0.25);
        WeakSpotPlacer.Layout l = layout(button);
        assertEquals(0.08, l.radius, 1e-9);
        assertEquals(0.03, l.edgeMargin, 1e-9);
        assertEquals(0.0947, l.minMoveDistance, 1e-3);
        assertMovesFarEnough(button, l);
    }

    private static void assertMovesFarEnough(FaceRect face, WeakSpotPlacer.Layout l) {
        Random random = new Random(3);
        double[] p = WeakSpotPlacer.place(face, l.radius, l.edgeMargin, face.centerU(), face.centerV(),
                l.minMoveDistance, random);
        for (int i = 0; i < 500; i++) {
            double[] next = WeakSpotPlacer.place(face, l.radius, l.edgeMargin, p[0], p[1], l.minMoveDistance, random);
            assertTrue(next[0] - l.radius >= face.minU - 1e-9 && next[0] + l.radius <= face.maxU + 1e-9);
            assertTrue(next[1] - l.radius >= face.minV - 1e-9 && next[1] + l.radius <= face.maxV + 1e-9);
            assertTrue(FaceMath.distance(p[0], p[1], next[0], next[1]) >= l.minMoveDistance - 1e-9);
            p = next;
        }
    }

    @Test
    public void maxRadiusWinsWhenFaceIsSmallerThanMinRadius() {
        // 短い辺 0.16 → 上限 0.056 が下限 0.08 を下回るので、上限が優先される（面からはみ出さない）
        FaceRect small = new FaceRect(0, 0, 0.5, 0.16);
        assertEquals(0.056, layout(small).radius, 1e-9);
    }

    @Test
    public void facesShorterThanMinFaceSizeGetNoSpot() {
        assertTrue(WeakSpotPlacer.isTooSmall(new FaceRect(0, 0, 1, 0.0625), 0.15));
        assertFalse(WeakSpotPlacer.isTooSmall(new FaceRect(0, 0, 0.375, 0.25), 0.15));
        assertFalse(WeakSpotPlacer.isTooSmall(new FaceRect(0, 0, 1, 0.15), 0.15));
    }

    @Test
    public void settingsChangeTheResult() {
        FaceRect button = new FaceRect(0, 0, 0.375, 0.25);
        assertEquals(0.05, WeakSpotPlacer.layout(button, RATIO, 0.05, MAX_RATIO, EDGE, MOVE).radius, 1e-9);
        assertEquals(0.05, WeakSpotPlacer.layout(button, RATIO, MIN_RADIUS, 0.2, EDGE, MOVE).radius, 1e-9);
        assertTrue(WeakSpotPlacer.isTooSmall(button, 0.3));
        assertFalse(WeakSpotPlacer.isTooSmall(button, 0.2));
    }

    @Test
    public void placedSpotStaysInsideAndMovesFarEnough() {
        Random random = new Random(1);
        double r = 0.14;
        double u = 0.5;
        double v = 0.5;
        for (int i = 0; i < 1000; i++) {
            double[] next = WeakSpotPlacer.place(FULL, r, 0.05, u, v, 0.4, random);
            assertTrue(next[0] - r >= 0 && next[0] + r <= 1);
            assertTrue(next[1] - r >= 0 && next[1] + r <= 1);
            assertTrue(FaceMath.distance(u, v, next[0], next[1]) >= 0.4);
            u = next[0];
            v = next[1];
        }
    }

    @Test
    public void edgeMarginKeepsSpotAwayFromEdges() {
        Random random = new Random(2);
        double r = 0.14;
        double edge = 0.1;
        for (int i = 0; i < 1000; i++) {
            double[] p = WeakSpotPlacer.place(FULL, r, edge, random.nextDouble(), random.nextDouble(), 0.4, random);
            assertTrue(p[0] - r >= edge - 1e-9 && p[0] + r <= 1 - edge + 1e-9);
            assertTrue(p[1] - r >= edge - 1e-9 && p[1] + r <= 1 - edge + 1e-9);
        }
    }

    @Test
    public void tinyFaceFallsBackToCenter() {
        FaceRect tiny = new FaceRect(0, 0, 0.1, 0.1);
        double[] p = WeakSpotPlacer.place(tiny, 0.1, 0.1, 0.05, 0.05, 0.4, new Random(1));
        assertEquals(0.05, p[0], 1e-9);
        assertEquals(0.05, p[1], 1e-9);
    }

    @Test
    public void hitTest() {
        assertTrue(WeakSpotPlacer.isHit(0.5, 0.5, 0.14, 0.6, 0.55));
        assertFalse(WeakSpotPlacer.isHit(0.5, 0.5, 0.14, 0.7, 0.5));
    }
}

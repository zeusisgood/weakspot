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

    @Test
    public void minRadiusAppliesOnSmallFaces() {
        FaceRect cocoaSide = new FaceRect(0, 0, 0.375, 0.5);
        assertEquals(0.08, WeakSpotPlacer.radius(cocoaSide, 0.14, 0.08), 1e-9);
        assertEquals(0.14, WeakSpotPlacer.radius(FULL, 0.14, 0.08), 1e-9);
    }

    @Test
    public void minRadiusNeverExceedsHalfTheFace() {
        FaceRect tiny = new FaceRect(0, 0, 0.1, 0.1);
        assertEquals(0.05, WeakSpotPlacer.radius(tiny, 0.14, 0.08), 1e-9);
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

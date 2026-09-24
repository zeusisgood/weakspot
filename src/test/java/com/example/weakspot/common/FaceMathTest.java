package com.example.weakspot.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FaceMathTest {

    private static final double EPS = 1e-9;

    @Test
    public void uvAndWorldRoundTrip() {
        for (int axis = 0; axis < 3; axis++) {
            double[] uv = FaceMath.toFaceUV(axis, 1.25, 2.5, 3.75);
            double[] world = {1.25, 2.5, 3.75};
            double[] back = FaceMath.toWorld(axis, world[axis], uv[0], uv[1]);
            assertArrayEquals(world, back, EPS);
        }
    }

    @Test
    public void uvAxesExcludeNormal() {
        assertArrayEquals(new double[] {3, 2}, FaceMath.toFaceUV(FaceMath.AXIS_X, 1, 2, 3), EPS);
        assertArrayEquals(new double[] {1, 3}, FaceMath.toFaceUV(FaceMath.AXIS_Y, 1, 2, 3), EPS);
        assertArrayEquals(new double[] {1, 2}, FaceMath.toFaceUV(FaceMath.AXIS_Z, 1, 2, 3), EPS);
    }

    @Test
    public void faceRectOfSlabSide() {
        FaceRect r = FaceMath.faceRect(FaceMath.AXIS_X, 10, 64, 5, 11, 64.5, 6);
        assertEquals(5, r.minU, EPS);
        assertEquals(6, r.maxU, EPS);
        assertEquals(64, r.minV, EPS);
        assertEquals(64.5, r.maxV, EPS);
    }

    @Test
    public void largestFaceOfLowCropIsTop() {
        // 小麦の成長段階0（高さ 0.125）
        assertEquals(FaceMath.AXIS_Y, FaceMath.largestFaceAxis(1, 0.125, 1));
    }

    @Test
    public void largestFacePrefersTopOnTies() {
        // 苗木の当たり判定（0.8 × 0.8 × 0.8）
        assertEquals(FaceMath.AXIS_Y, FaceMath.largestFaceAxis(0.8, 0.8, 0.8));
    }

    @Test
    public void largestFaceOfThinTallBoxIsSide() {
        assertEquals(FaceMath.AXIS_X, FaceMath.largestFaceAxis(0.2, 1, 1));
        assertEquals(FaceMath.AXIS_Z, FaceMath.largestFaceAxis(1, 1, 0.2));
    }
}

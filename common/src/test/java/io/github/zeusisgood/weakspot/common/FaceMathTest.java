package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    private static final int NONE = -1;

    @Test
    public void growthFaceOfLowCropStaysTopWhateverTheAim() {
        // 小麦など上面が一番大きい作物は、照準や今の面に関係なく上面
        assertEquals(FaceMath.AXIS_Y, FaceMath.growthFaceAxis(1, 0.125, 1, NONE, FaceMath.AXIS_X, 3, 0));
        assertEquals(FaceMath.AXIS_Y, FaceMath.growthFaceAxis(1, 0.875, 1, FaceMath.AXIS_Z, FaceMath.AXIS_Z, 0, 3));
    }

    @Test
    public void growthFaceOfSaplingStaysTop() {
        assertEquals(FaceMath.AXIS_Y, FaceMath.growthFaceAxis(0.8, 0.8, 0.8, FaceMath.AXIS_X, FaceMath.AXIS_Z, 0, 3));
    }

    @Test
    public void growthFaceOfSugarCanePrefersAimedSide() {
        // サトウキビ（0.75 × 1 × 0.75）の側面は4つとも同じ大きさ
        assertEquals(FaceMath.AXIS_Z, FaceMath.growthFaceAxis(0.75, 1, 0.75, NONE, FaceMath.AXIS_Z, 3, 0.1));
        assertEquals(FaceMath.AXIS_X, FaceMath.growthFaceAxis(0.75, 1, 0.75, NONE, FaceMath.AXIS_X, 0.1, 3));
    }

    @Test
    public void growthFaceKeepsVisibleCurrentSide() {
        // 照準が隣の側面へ移っても、今の面が見えていれば変えない（ちらつかない）
        assertEquals(FaceMath.AXIS_X, FaceMath.growthFaceAxis(0.75, 1, 0.75, FaceMath.AXIS_X, FaceMath.AXIS_Z, 1, 1));
    }

    @Test
    public void growthFaceFromTopAimFollowsEye() {
        // 上面を狙っているときは、視点のずれが大きいほうの側面
        assertEquals(FaceMath.AXIS_Z, FaceMath.growthFaceAxis(0.75, 1, 0.75, NONE, FaceMath.AXIS_Y, 0.2, -3));
        assertEquals(FaceMath.AXIS_X, FaceMath.growthFaceAxis(0.75, 1, 0.75, NONE, FaceMath.AXIS_Y, 3, 0.2));
    }

    @Test
    public void growthFaceIgnoresSmallerSides() {
        // X 面だけが一番大きい箱では、Z 面を狙っていても、Z 面が今の面でも X 面
        assertEquals(FaceMath.AXIS_X, FaceMath.growthFaceAxis(0.2, 1, 1, FaceMath.AXIS_Z, FaceMath.AXIS_Z, 0, 3));
    }

    @Test
    public void nearestFacePicksTheFaceTheAimIsOn() {
        double[] min = {0, 0, 0};
        double[] max = {0.6, 1.4, 0.6};
        assertArrayEquals(new int[] {FaceMath.AXIS_Y, 1}, FaceMath.nearestFace(new double[] {0.3, 1.4, 0.3}, min, max));
        assertArrayEquals(new int[] {FaceMath.AXIS_X, -1}, FaceMath.nearestFace(new double[] {0, 0.7, 0.3}, min, max));
        assertArrayEquals(new int[] {FaceMath.AXIS_Z, 1}, FaceMath.nearestFace(new double[] {0.3, 0.7, 0.6}, min, max));
        assertArrayEquals(new int[] {FaceMath.AXIS_Y, -1}, FaceMath.nearestFace(new double[] {0.3, 0.0, 0.3}, min, max));
    }

    private static final double[] BOX_MIN = {0, 0, 0};
    private static final double[] BOX_MAX = {1, 1, 1};

    @Test
    public void rayFromTheFrontEntersTheFrontFace() {
        double[] eye = {0.5, 0.5, -2};
        assertArrayEquals(new int[] {FaceMath.AXIS_Z, -1},
                FaceMath.rayEntryFace(eye, new double[] {0.1, -0.05, 1}, BOX_MIN, BOX_MAX));
    }

    @Test
    public void rayFromAboveAtASteepAngleEntersTheTop() {
        double[] eye = {0.5, 2.5, -0.5};
        assertArrayEquals(new int[] {FaceMath.AXIS_Y, 1},
                FaceMath.rayEntryFace(eye, new double[] {0, -1, 0.4}, BOX_MIN, BOX_MAX));
    }

    @Test
    public void rayThatHitsARecessFromTheFrontStillEntersTheFront() {
        // 凹んだ所の横の面（X 向き）に当たる視線でも、外枠の箱には正面から入る
        double[] eye = {0.3, 0.5, -1};
        assertArrayEquals(new int[] {FaceMath.AXIS_Z, -1},
                FaceMath.rayEntryFace(eye, new double[] {0.3, 0, 1}, BOX_MIN, BOX_MAX));
    }

    @Test
    public void rayFromInsideTheBoxEntersNoFace() {
        assertNull(FaceMath.rayEntryFace(new double[] {0.5, 0.5, 0.5}, new double[] {0, 0, 1}, BOX_MIN, BOX_MAX));
    }

    @Test
    public void rayThatMissesTheBoxEntersNoFace() {
        assertNull(FaceMath.rayEntryFace(new double[] {0.5, 0.5, -2}, new double[] {1, 0, 0.2}, BOX_MIN, BOX_MAX));
        assertNull(FaceMath.rayEntryFace(new double[] {2, 0.5, 0.5}, new double[] {0, 0, 1}, BOX_MIN, BOX_MAX));
    }

    @Test
    public void boxBehindTheEyeEntersNoFace() {
        assertNull(FaceMath.rayEntryFace(new double[] {0.5, 0.5, -2}, new double[] {0, 0, -1}, BOX_MIN, BOX_MAX));
    }

    @Test
    public void rayMeetsTheFacePlane() {
        double[] p = FaceMath.rayPlane(new double[] {0.5, 1.5, -2}, new double[] {0, -0.25, 1}, FaceMath.AXIS_Z, 0);
        assertArrayEquals(new double[] {0.5, 1.0, 0}, p, EPS);
    }

    @Test
    public void planeBehindTheEyeOrParallelIsAMiss() {
        assertNull(FaceMath.rayPlane(new double[] {0.5, 0.5, 2}, new double[] {0, 0, 1}, FaceMath.AXIS_Z, 0));
        assertNull(FaceMath.rayPlane(new double[] {0.5, 0.5, -2}, new double[] {1, 0, 0}, FaceMath.AXIS_Z, 0));
    }

    @Test
    public void neighborBoxProjectsOntoTheFace() {
        // 北の面（Z 向き）の手前の、太さ 0.25 のパイプ
        FaceRect r = FaceMath.faceRect(FaceMath.AXIS_Z, 0.375, 0.375, -1, 0.625, 0.625, 0);
        assertEquals(0.375, r.minU, EPS);
        assertEquals(0.375, r.minV, EPS);
        assertEquals(0.625, r.maxU, EPS);
        assertEquals(0.625, r.maxV, EPS);
    }
}

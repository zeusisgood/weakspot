package com.example.weakspot.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlockHealthBarTest {

    private static final double EPS = 1e-9;
    private static final FaceRect UNIT = new FaceRect(0, 0, 1, 1);

    @Test
    public void remainingIsOneMinusProgressClamped() {
        assertEquals(1.0, BlockHealthBar.remaining(0), EPS);
        assertEquals(0.75, BlockHealthBar.remaining(0.25), EPS);
        assertEquals(0.0, BlockHealthBar.remaining(1), EPS);
        assertEquals(0.0, BlockHealthBar.remaining(1.3), EPS);
        assertEquals(1.0, BlockHealthBar.remaining(-0.2), EPS);
    }

    @Test
    public void interpolatesBetweenTicks() {
        assertEquals(0.2, BlockHealthBar.interpolate(0.2, 0.4, 0), EPS);
        assertEquals(0.3, BlockHealthBar.interpolate(0.2, 0.4, 0.5), EPS);
        assertEquals(0.35, BlockHealthBar.interpolate(0.2, 0.4, 0.75), EPS);
        assertEquals(0.4, BlockHealthBar.interpolate(0.2, 0.4, 1), EPS);
    }

    @Test
    public void unitSideFaceBarSizeAndPlace() {
        // 北の面（-Z）: (u, v) = (x, y)。下の辺は y = 0
        BlockHealthBar bar = BlockHealthBar.place(FaceMath.AXIS_Z, -1, UNIT, 0.5, -3);
        assertTrue(bar.alongU);
        double[] full = bar.rect(1);
        assertEquals(0.8, full[2] - full[0], EPS);
        assertEquals(0.06, full[3] - full[1], EPS);
        assertEquals(0.05, (full[1] + full[3]) / 2, EPS);
        assertEquals(0.5, (full[0] + full[2]) / 2, EPS);
    }

    @Test
    public void sideFaceLengthFollowsTheBox() {
        // 下のハーフブロックの東の面（+X）: (u, v) = (z, y)、高さ 0.5
        BlockHealthBar slab = BlockHealthBar.place(FaceMath.AXIS_X, 1, new FaceRect(10, 64, 11, 64.5), 13, 10.5);
        double[] full = slab.rect(1);
        assertEquals(0.8, full[2] - full[0], EPS);
        assertEquals(64.05, (full[1] + full[3]) / 2, EPS);
        // 幅 0.25 の面（フェンスの柱など）は 0.2
        BlockHealthBar post = BlockHealthBar.place(FaceMath.AXIS_Z, 1, new FaceRect(0.375, 0, 0.625, 1), 0.5, 3);
        double[] p = post.rect(1);
        assertEquals(0.2, p[2] - p[0], EPS);
        assertEquals(0.5, (p[0] + p[2]) / 2, EPS);
    }

    @Test
    public void sideFaceShrinksFromTheViewersRight() {
        // 北の面を見るプレイヤーは南（+Z）を向き、右手は西（-X）。左端は x = 0.9
        assertArrayEquals(new double[] {0.5, 0.02, 0.9, 0.08},
                BlockHealthBar.place(FaceMath.AXIS_Z, -1, UNIT, 0.5, -3).rect(0.5), EPS);
        // 南の面: 北を向き、右手は東（+X）。左端は x = 0.1
        assertArrayEquals(new double[] {0.1, 0.02, 0.5, 0.08},
                BlockHealthBar.place(FaceMath.AXIS_Z, 1, UNIT, 0.5, 4).rect(0.5), EPS);
        // 東の面（u = z）: 西を向き、右手は北（-Z）。左端は z = 0.9
        assertArrayEquals(new double[] {0.5, 0.02, 0.9, 0.08},
                BlockHealthBar.place(FaceMath.AXIS_X, 1, UNIT, 4, 0.5).rect(0.5), EPS);
        // 西の面: 東を向き、右手は南（+Z）。左端は z = 0.1
        assertArrayEquals(new double[] {0.1, 0.02, 0.5, 0.08},
                BlockHealthBar.place(FaceMath.AXIS_X, -1, UNIT, -3, 0.5).rect(0.5), EPS);
    }

    @Test
    public void topFaceUsesTheEdgeNearestThePlayer() {
        // 上面: (u, v) = (x, z)。プレイヤーが東西南北のどこにいるかで辺が変わる
        BlockHealthBar north = BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 0.6, -2);
        assertTrue(north.alongU);
        assertEquals(0.05, north.across, EPS);
        BlockHealthBar south = BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 0.4, 3);
        assertTrue(south.alongU);
        assertEquals(0.95, south.across, EPS);
        BlockHealthBar west = BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, -2, 0.3);
        assertFalse(west.alongU);
        assertEquals(0.05, west.across, EPS);
        BlockHealthBar east = BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 3, 0.7);
        assertFalse(east.alongU);
        assertEquals(0.95, east.across, EPS);
        // 斜めにいるときは、より外側にはみ出している辺
        assertFalse(BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 3, 1.5).alongU);
        assertTrue(BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 1.5, 3).alongU);
        // 真上に立っていても、一番近い辺を選ぶ
        assertEquals(0.95, BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 0.5, 0.8).across, EPS);
    }

    @Test
    public void topAndBottomFacesShrinkFromTheViewersRight() {
        // 北の辺: 南を向き、右手は西（-X）。左端は x = 0.9
        assertArrayEquals(new double[] {0.5, 0.02, 0.9, 0.08},
                BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 0.5, -2).rect(0.5), EPS);
        // 南の辺: 北を向き、右手は東（+X）
        assertArrayEquals(new double[] {0.1, 0.92, 0.5, 0.98},
                BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 0.5, 3).rect(0.5), EPS);
        // 西の辺: 東を向き、右手は南（+Z）。rect は {minU(x), minV(z), maxU, maxV}
        assertArrayEquals(new double[] {0.02, 0.1, 0.08, 0.5},
                BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, -2, 0.5).rect(0.5), EPS);
        // 東の辺: 西を向き、右手は北（-Z）
        assertArrayEquals(new double[] {0.92, 0.5, 0.98, 0.9},
                BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 3, 0.5).rect(0.5), EPS);
        // 下面（天井）も、同じ辺と左右
        assertArrayEquals(BlockHealthBar.place(FaceMath.AXIS_Y, 1, UNIT, 3, 0.5).rect(0.5),
                BlockHealthBar.place(FaceMath.AXIS_Y, -1, UNIT, 3, 0.5).rect(0.5), EPS);
    }

    @Test
    public void emptyBarHasNoLength() {
        double[] r = BlockHealthBar.place(FaceMath.AXIS_Z, -1, UNIT, 0.5, -3).rect(0);
        assertEquals(0, r[2] - r[0], EPS);
    }
}

package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MeleeAreaTest {

    @Test
    public void sideFacesSkipTheBottomFortyPercent() {
        // ゾンビの前の面（幅 0.6、高さ 1.95）
        FaceRect front = MeleeArea.area(0.6, 1.95, FaceMath.AXIS_Z, true);
        assertEquals(0.0, front.minU, 1e-9);
        assertEquals(0.6, front.maxU, 1e-9);
        assertEquals(0.78, front.minV, 1e-9);
        assertEquals(1.95, front.maxV, 1e-9);
        FaceRect side = MeleeArea.area(0.6, 1.95, FaceMath.AXIS_X, false);
        assertEquals(0.78, side.minV, 1e-9);
        assertEquals(1.95 * 0.6, side.height(), 1e-9);
    }

    @Test
    public void topFaceIsWhole() {
        FaceRect top = MeleeArea.area(0.6, 0.6, FaceMath.AXIS_Y, true);
        assertEquals(0.0, top.minV, 1e-9);
        assertEquals(0.6, top.height(), 1e-9);
        assertEquals(0.6, top.width(), 1e-9);
    }

    @Test
    public void bottomFaceHasNoArea() {
        assertNull(MeleeArea.area(0.6, 0.6, FaceMath.AXIS_Y, false));
    }
}

package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SleepSpotAreaTest {

    @Test
    public void centerFortyPercent() {
        double[] a = SleepSpotArea.area(400, 240, 12);
        assertEquals(120, a[0], 1e-9);
        assertEquals(280, a[1], 1e-9);
        assertEquals(72, a[2], 1e-9);
        assertEquals(168, a[3], 1e-9);
    }

    @Test
    public void bottomStaysClearOfTheButtons() {
        // 下の 1/4（高さ 100 なら 75 から下）に、半径 12 のマーカーがかからない
        double[] a = SleepSpotArea.area(400, 100, 12);
        assertEquals(63, a[3], 1e-9);
    }

    @Test
    public void minMoveFitsTheSmallArea() {
        assertEquals(36, SleepSpotArea.minMove(400, 240, 12), 1e-9);
        assertEquals(30, SleepSpotArea.minMove(150, 100, 12), 1e-9);
    }
}

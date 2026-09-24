package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GrowthRoomTest {

    @Test
    public void columnBelowMaxHeightWithAirAboveHasRoom() {
        assertTrue(GrowthRoom.hasColumnRoom(1, true, true));
        assertTrue(GrowthRoom.hasColumnRoom(2, true, true));
    }

    @Test
    public void columnAtMaxHeightHasNoRoom() {
        assertFalse(GrowthRoom.hasColumnRoom(3, true, true));
        assertFalse(GrowthRoom.hasColumnRoom(4, true, true));
    }

    @Test
    public void columnWithoutAirAboveHasNoRoom() {
        assertFalse(GrowthRoom.hasColumnRoom(1, false, true));
        assertFalse(GrowthRoom.hasColumnRoom(2, false, true));
    }

    @Test
    public void singleBlockWithoutBaseHasNoRoom() {
        // 高さ1のサトウキビは、土台（下の土と隣の水）がなければ randomTick で壊れる
        assertFalse(GrowthRoom.hasColumnRoom(1, true, false));
    }

    @Test
    public void baseIsIgnoredAboveHeightOne() {
        // バニラは、下が同じブロックなら土台を確かめずに伸ばす
        assertTrue(GrowthRoom.hasColumnRoom(2, true, false));
    }

    @Test
    public void netherWartHasRoomUntilLastStage() {
        for (int stage = 0; stage <= 2; stage++) {
            assertTrue(GrowthRoom.hasStageRoom(stage, 3));
        }
        assertFalse(GrowthRoom.hasStageRoom(3, 3));
    }

    @Test
    public void countRunStopsAtFirstDifferentBlock() {
        // offset 1, 2 が同じブロックで、3 が違う
        assertEquals(2, GrowthRoom.countRun(k -> k <= 2, 10));
        assertEquals(0, GrowthRoom.countRun(k -> false, 10));
    }

    @Test
    public void countRunStopsAtLimit() {
        assertEquals(3, GrowthRoom.countRun(k -> true, 3));
    }
}

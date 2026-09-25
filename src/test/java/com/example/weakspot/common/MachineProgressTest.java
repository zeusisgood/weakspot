package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MachineProgressTest {

    @Test
    public void ratioIsClampedAndSafe() {
        assertEquals(0.5, MachineProgress.ratio(100, 200), 1e-9);
        assertEquals(0, MachineProgress.ratio(5, 0), 1e-9);
        assertEquals(1, MachineProgress.ratio(300, 200), 1e-9);
        assertEquals(0, MachineProgress.ratio(-1, 200), 1e-9);
    }

    @Test
    public void brewingCountsDown() {
        assertEquals(0, MachineProgress.brewing(0), 1e-9);
        assertEquals(0, MachineProgress.brewing(400), 1e-9);
        assertEquals(0.75, MachineProgress.brewing(100), 1e-9);
    }

    @Test
    public void spawnerRemembersTheStart() {
        MachineProgress.SpawnerDelay delay = new MachineProgress.SpawnerDelay();
        assertEquals(0, delay.update(400), 1e-9);
        assertEquals(0.5, delay.update(200), 1e-9);
        assertEquals(0.75, delay.update(100), 1e-9);
        // 湧いて、待ち時間が決め直された
        assertEquals(0, delay.update(600), 1e-9);
        assertEquals(0.5, delay.update(300), 1e-9);
    }
}

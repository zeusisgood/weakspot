package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MachineParticlesTest {

    @Test
    public void colorFollowsTheComboSteps() {
        double base = 4.0;
        assertEquals(0x55FFFF, MachineParticles.colorFor(MachineParticles.comboFactor(4, base)));
        assertEquals(0xFFFF55, MachineParticles.colorFor(MachineParticles.comboFactor(5, base)));
        assertEquals(0xFFAA00, MachineParticles.colorFor(MachineParticles.comboFactor(6, base)));
        assertEquals(0xFF5555, MachineParticles.colorFor(MachineParticles.comboFactor(8, base)));
        assertEquals(0xFF55FF, MachineParticles.colorFor(MachineParticles.comboFactor(10, base)));
        assertEquals(0xAA55FF, MachineParticles.colorFor(MachineParticles.comboFactor(12, base)));
        assertEquals(0xFFD700, MachineParticles.colorFor(MachineParticles.comboFactor(16, base)));
    }

    @Test
    public void cappedMultiplierShowsTheLowerColor() {
        // 上限 8 で抑えられたら、コンボ 1000 でも赤（8倍速）
        assertEquals(0xFF5555, MachineParticles.colorFor(MachineParticles.comboFactor(Math.min(16, 8), 4)));
    }

    @Test
    public void particleCountScalesWithSpeed() {
        assertEquals(10, particlesOver(4, 20));
        assertEquals(20, particlesOver(8, 20));
        assertEquals(40, particlesOver(16, 20));
    }

    private static int particlesOver(double multiplier, int ticks) {
        MachineBoost boost = new MachineBoost();
        boost.hit(multiplier, ticks);
        int total = 0;
        while (boost.isActive()) {
            total += boost.nextParticles();
            boost.nextTickCalls();
        }
        return total;
    }
}

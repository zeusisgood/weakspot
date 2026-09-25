package com.example.weakspot.common;

/**
 * 加速中の機械のまわりに出す粒子の色と量（1.5.3）。色はコンボの掛け数（今の倍率 ÷ machineBoostMultiplier）の段階で、
 * 量は速いほど多く、1 tick に 倍率 ÷ PARTICLES_DIVISOR 個（端数は MachineBoost が持ち越す）。
 */
public final class MachineParticles {

    /** 1 tick に出す数 = 倍率 ÷ この数（4倍速で 2 tick に 1 個、8倍速で 1 個、16倍速で 2 個）。 */
    public static final double PARTICLES_DIVISOR = 8;

    /** この掛け数以上で、同じ位置の色になる（MachineComboBoost の段階と同じ）。 */
    private static final double[] STEPS = {1.25, 1.5, 2.0, 2.5, 3.0, 4.0};
    private static final int[] COLORS = {0xFFFF55, 0xFFAA00, 0xFF5555, 0xFF55FF, 0xAA55FF, 0xFFD700};
    private static final int BASE_COLOR = 0x55FFFF;

    private MachineParticles() {
    }

    /** 掛け数（倍率 ÷ machineBoostMultiplier）の色。浮動小数の誤差を見込んで、少しだけ甘く比べる。 */
    public static int colorFor(double comboFactor) {
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (comboFactor >= STEPS[i] - 1e-9) {
                return COLORS[i];
            }
        }
        return BASE_COLOR;
    }

    /** 倍率と machineBoostMultiplier から、掛け数を求める。 */
    public static double comboFactor(double multiplier, double baseMultiplier) {
        return baseMultiplier <= 0 ? 1 : multiplier / baseMultiplier;
    }
}

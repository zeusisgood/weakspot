package io.github.zeusisgood.weakspot.common;

public final class MachineParticles {

    /** 1 tick に出す数 = 倍率 ÷ この数（4倍速で 2 tick に 1 個、16倍速で 2 個）。端数は MachineBoost が持ち越す。 */
    public static final double PARTICLES_DIVISOR = 8;

    /** MachineComboBoost の段階と同じ。 */
    private static final double[] STEPS = {1.25, 1.5, 2.0, 2.5, 3.0, 4.0};
    private static final int[] COLORS = {0xFFFF55, 0xFFAA00, 0xFF5555, 0xFF55FF, 0xAA55FF, 0xFFD700};
    private static final int BASE_COLOR = 0x55FFFF;

    private MachineParticles() {
    }

    /** 浮動小数の誤差を見込んで、少しだけ甘く比べる。 */
    public static int colorFor(double comboFactor) {
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (comboFactor >= STEPS[i] - 1e-9) {
                return COLORS[i];
            }
        }
        return BASE_COLOR;
    }

    public static double comboFactor(double multiplier, double baseMultiplier) {
        return baseMultiplier <= 0 ? 1 : multiplier / baseMultiplier;
    }
}

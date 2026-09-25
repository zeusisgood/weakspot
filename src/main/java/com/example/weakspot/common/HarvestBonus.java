package com.example.weakspot.common;

import java.util.Random;

/**
 * 収穫のコンボのおまけ（1.7.0）。収穫物の数にコンボの掛け数（MachineComboBoost.factor）を掛け、端数は確率で 1 つ足す
 * （×1.25 なら 4 回に 1 回 +1）。上限はない。Minecraft に依存しない。
 */
public final class HarvestBonus {

    private HarvestBonus() {
    }

    /** count 個に factor を掛けたあとの数。factor が 1 以下なら count のまま。 */
    public static int apply(int count, double factor, Random random) {
        if (count <= 0 || factor <= 1) {
            return Math.max(0, count);
        }
        double scaled = count * factor;
        int whole = (int) Math.floor(scaled);
        double fraction = scaled - whole;
        return whole + (fraction > 0 && random.nextDouble() < fraction ? 1 : 0);
    }
}

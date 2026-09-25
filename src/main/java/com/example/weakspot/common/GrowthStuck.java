package com.example.weakspot.common;

/**
 * 成長の弱点に当てても育たないときの知らせを出すかの判定（1.4.3。プレイヤーごとに1つ）。
 * 暗い（原因が分かる）ときは最初のヒットで、分からないときは stuckHits 回続けて状態が変わらなかったときに知らせる。
 * 同じブロックへは SAME_BLOCK_COOLDOWN tick、どのブロックでも ANY_COOLDOWN tick に1回まで。
 */
public final class GrowthStuck {

    public enum Warning {
        NONE,
        /** 暗くて育たない。 */
        DARK,
        /** 何らかの外部要因で育たない。 */
        STUCK
    }

    public static final int SAME_BLOCK_COOLDOWN = 600;
    public static final int ANY_COOLDOWN = 100;

    private static final long NEVER = Long.MIN_VALUE / 2;

    private long block = Long.MIN_VALUE;
    private int unchangedHits;
    private long lastWarnTick = NEVER;
    private long lastWarnBlock = Long.MIN_VALUE;

    /**
     * 成長のヒットを受け付けて効果をかけたあとに呼ぶ。
     *
     * @param blockKey ブロックの位置（BlockPos#toLong など）
     * @param changed 効果の前後で状態が変わったか
     * @param dark 暗くて育たない（原因が分かる）か
     * @param stuckHits 何回続けて変わらなかったら知らせるか
     * @param tick 今の tick
     * @return 出す知らせ。出したことは覚える
     */
    public Warning onHit(long blockKey, boolean changed, boolean dark, int stuckHits, long tick) {
        if (blockKey != block) {
            block = blockKey;
            unchangedHits = 0;
        }
        if (changed) {
            unchangedHits = 0;
            return Warning.NONE;
        }
        unchangedHits++;
        Warning warning = dark ? Warning.DARK : unchangedHits >= stuckHits ? Warning.STUCK : Warning.NONE;
        if (warning == Warning.NONE || !canWarn(blockKey, tick)) {
            return Warning.NONE;
        }
        lastWarnTick = tick;
        lastWarnBlock = blockKey;
        unchangedHits = 0;
        return warning;
    }

    /** 今までに続けて変わらなかった回数（テスト用）。 */
    int unchangedHits() {
        return unchangedHits;
    }

    private boolean canWarn(long blockKey, long tick) {
        long since = tick - lastWarnTick;
        if (since < ANY_COOLDOWN) {
            return false;
        }
        return blockKey != lastWarnBlock || since >= SAME_BLOCK_COOLDOWN;
    }
}

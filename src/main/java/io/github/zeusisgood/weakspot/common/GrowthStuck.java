package io.github.zeusisgood.weakspot.common;

/**
 * 成長の弱点に当てても育たないときの知らせ（プレイヤーごとに 1 つ）。暗い（原因が分かる）ときは最初のヒットで、
 * 分からないときは続けて変わらなかったときに知らせる。
 */
public final class GrowthStuck {

    public enum Warning {
        NONE,
        DARK,
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
    * 成長のヒットを受け付けて効果をかけたあとに呼ぶ。出した知らせは覚える。
    *
    * @param blockKey BlockPos#toLong など
    * @param changed 効果の前後で状態が変わったか
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

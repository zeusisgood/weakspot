package com.example.weakspot.common;

/**
 * 弱点採掘の統計。Minecraft に依存しない集計だけを持ち、保存は呼び出し側が行う（Gson でそのまま直列化できる形）。
 */
public final class MiningStats {

    /** 弱点に当てた回数。 */
    public long hits;
    /** 弱点が出るブロック（壊せて、一瞬では壊れないもの）を壊した数。 */
    public long blocksBroken;
    /** そのうち、弱点に1回以上当てて壊した数。 */
    public long blocksBrokenWithHit;
    /** 1つのブロックで当てた回数の最大。 */
    public long maxHitsOnBlock;
    /** ヒットで得た追加進捗の合計（通常速度の tick 数）。短縮できた時間の推定に使う。 */
    public double savedTicks;

    public void recordHit(double extraTicks) {
        hits++;
        savedTicks += Math.max(0, extraTicks);
    }

    public void recordBlockBroken(int hitsOnBlock) {
        blocksBroken++;
        if (hitsOnBlock > 0) {
            blocksBrokenWithHit++;
        }
        maxHitsOnBlock = Math.max(maxHitsOnBlock, hitsOnBlock);
    }

    /** 壊したブロック1つあたりの平均ヒット数。まだ1つも壊していなければ NaN。 */
    public double averageHitsPerBlock() {
        return blocksBroken == 0 ? Double.NaN : (double) hits / blocksBroken;
    }

    public double savedSeconds() {
        return savedTicks / 20.0;
    }

    public void reset() {
        hits = 0;
        blocksBroken = 0;
        blocksBrokenWithHit = 0;
        maxHitsOnBlock = 0;
        savedTicks = 0;
    }
}

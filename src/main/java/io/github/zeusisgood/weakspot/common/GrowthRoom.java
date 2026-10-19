package io.github.zeusisgood.weakspot.common;

import java.util.function.IntPredicate;

/** IGrowable を持たない植物（サトウキビ、サボテン、ネザーウォート）。条件はバニラ 1.12.2 の updateTick と同じ。 */
public final class GrowthRoom {

    /** 一番上から下へ数えた高さがこれ未満なら伸びる。 */
    public static final int MAX_COLUMN_HEIGHT = 3;

    private GrowthRoom() {
    }

    /** バニラは、高さ 1 のときだけ土台を確かめ、満たさなければ壊すので、baseHolds は高さ 1 のときだけ見る。 */
    public static boolean hasColumnRoom(int height, boolean airAbove, boolean baseHolds) {
        return height < MAX_COLUMN_HEIGHT && airAbove && (height != 1 || baseHolds);
    }

    /** 柱で、あるブロックの上（または下）に同じブロックがいくつ続くか。 */
    public static int countRun(IntPredicate sameAt, int limit) {
        int n = 0;
        while (n < limit && sameAt.test(n + 1)) {
            n++;
        }
        return n;
    }
}

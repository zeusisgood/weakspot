package com.example.weakspot.common;

import java.util.function.IntPredicate;

/**
 * IGrowable を持たない植物（サトウキビ、サボテン、ネザーウォート）の「育てる余地」の判定。
 * Minecraft のクラスに依存しない。条件はバニラ 1.12.2 の updateTick と同じ。
 */
public final class GrowthRoom {

    /** サトウキビ・サボテンが自然に伸びる柱の高さの上限（一番上から下へ数えた高さがこれ未満なら伸びる）。 */
    public static final int MAX_COLUMN_HEIGHT = 3;

    private GrowthRoom() {
    }

    /**
     * 柱の植物に育てる余地があるか。
     * baseHolds は、柱の一番下が土台の条件を満たすか。バニラは、下が同じブロックでないとき（高さ1）だけ
     * 土台を確かめ、満たさなければ壊すので、高さ1のときだけ見る。
     */
    public static boolean hasColumnRoom(int height, boolean airAbove, boolean baseHolds) {
        return height < MAX_COLUMN_HEIGHT && airAbove && (height != 1 || baseHolds);
    }

    /** 成長段階の植物に育てる余地があるか（最後の段階でない）。 */
    public static boolean hasStageRoom(int stage, int lastStage) {
        return stage < lastStage;
    }

    /**
     * offset 1, 2, ... の順に sameAt が true の間数えた数（limit まで）。
     * 柱で、あるブロックの上（または下）に同じブロックがいくつ続くかを数えるのに使う。
     */
    public static int countRun(IntPredicate sameAt, int limit) {
        int n = 0;
        while (n < limit && sameAt.test(n + 1)) {
            n++;
        }
        return n;
    }
}

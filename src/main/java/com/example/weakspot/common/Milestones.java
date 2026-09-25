package com.example.weakspot.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 採掘ヒットの累計に応じた節目の判定（耐久回復の精算は RepairSettlement）。
 * 累計は1ヒットずつ増えるので、「節目の数字に達した」は、増えた後の値が一致するかで判定する。
 */
public final class Milestones {

    private Milestones() {
    }

    /** 累計がちょうど達した節目の番号（milestones の添字）。同じ数字が複数あれば、すべて返す。 */
    public static List<Integer> reached(int[] milestones, long totalHits) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < milestones.length; i++) {
            if (milestones[i] > 0 && milestones[i] == totalHits) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    /**
     * 繰り返しの節目も含めて、累計がちょうど達した節目（1.7.0）。返すのは {節目の数, 量の添字} の一覧。
     * milestones の一番大きい数より先は、repeatInterval ごと（0 以下なら繰り返さない）に節目にし、量は最後の添字を使う。
     */
    public static List<long[]> reachedWithRepeat(int[] milestones, int repeatInterval, long totalHits) {
        List<long[]> reached = new ArrayList<>();
        for (int index : reached(milestones, totalHits)) {
            reached.add(new long[] {totalHits, index});
        }
        long max = 0;
        for (int m : milestones) {
            max = Math.max(max, m);
        }
        if (repeatInterval > 0 && max > 0 && milestones.length > 0 && totalHits > max
                && (totalHits - max) % repeatInterval == 0) {
            reached.add(new long[] {totalHits, milestones.length - 1});
        }
        return reached;
    }

    /** 7 だけが並ぶ数（777、7777 …）か。節目の演出を虹色にする（1.7.0）。 */
    public static boolean isLucky(long milestone) {
        if (milestone < 7) {
            return false;
        }
        for (long n = milestone; n > 0; n /= 10) {
            if (n % 10 != 7) {
                return false;
            }
        }
        return true;
    }

    /** 節目の順に対応する量。足りない分は0。 */
    public static int amountAt(int[] amounts, int index) {
        return index < amounts.length ? Math.max(0, amounts[index]) : 0;
    }

    /** 回復したあとの損傷値（0 未満にはしない）。 */
    public static int repairedDamage(int damage, int amount) {
        return Math.max(0, damage - Math.max(0, amount));
    }
}

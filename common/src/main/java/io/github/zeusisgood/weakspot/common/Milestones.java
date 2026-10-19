package io.github.zeusisgood.weakspot.common;

import java.util.ArrayList;
import java.util.List;

/** 累計は 1 ヒットずつ増えるので、「節目に達した」は、増えた後の値が一致するかで決める。 */
public final class Milestones {

    private Milestones() {
    }

    /** milestones の添字を返す。同じ数字が複数あれば、すべて返す。 */
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
    * {節目の数, 量の添字} の一覧を返す。milestones の一番大きい数より先は、repeatInterval ごと（0 以下なら繰り返さない）に
    * 節目にし、量は最後の添字を使う。
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

    /** 7 だけが並ぶ数（777、7777 …）。節目の演出を虹色にする。 */
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

    /** 足りない分は 0。 */
    public static int amountAt(int[] amounts, int index) {
        return index < amounts.length ? Math.max(0, amounts[index]) : 0;
    }

    /** 0 未満にはしない。 */
    public static int repairedDamage(int damage, int amount) {
        return Math.max(0, damage - Math.max(0, amount));
    }
}

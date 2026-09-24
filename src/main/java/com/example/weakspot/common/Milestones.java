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

    /** 節目の順に対応する量。足りない分は0。 */
    public static int amountAt(int[] amounts, int index) {
        return index < amounts.length ? Math.max(0, amounts[index]) : 0;
    }

    /** 回復したあとの損傷値（0 未満にはしない）。 */
    public static int repairedDamage(int damage, int amount) {
        return Math.max(0, damage - Math.max(0, amount));
    }
}

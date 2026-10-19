package io.github.zeusisgood.weakspot.common;

/**
* 採掘ヒットはブロックごとに未確定として持ち、そのブロックを壊したときにだけ、ここで回復の数え方に足す。
* 壊さずにやめたブロックのヒットは、呼び出し側で捨てる。
*/
public final class RepairSettlement {

    /** 次の破壊へ持ち越す数え方（hitsPerRepair 未満）。 */
    public final int carry;
    public final int repair;

    private RepairSettlement(int carry, int repair) {
        this.carry = carry;
        this.repair = repair;
    }

    /**
    * 回復は maxRepairPerBreak と、ツールの減っている耐久（currentDamage）までで、上限で回復できなかった分は持ち越さない。
    * ツールの耐久が減っていなくても、数え方は進む。hitsPerRepair が 0 以下なら回復せず、数え方も 0 にする。
    */
    public static RepairSettlement settle(int carry, int confirmedHits, int hitsPerRepair, int repairPerStep,
            int maxRepairPerBreak, int currentDamage) {
        if (hitsPerRepair <= 0) {
            return new RepairSettlement(0, 0);
        }
        long count = (long) Math.max(0, carry) + Math.max(0, confirmedHits);
        long steps = count / hitsPerRepair;
        long amount = Math.min(steps * Math.max(0, repairPerStep), (long) Math.max(0, maxRepairPerBreak));
        amount = Math.min(amount, (long) Math.max(0, currentDamage));
        return new RepairSettlement((int) (count % hitsPerRepair), (int) amount);
    }
}

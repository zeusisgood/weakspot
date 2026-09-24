package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RepairSettlementTest {

    private static final int HITS_PER_REPAIR = 5;
    private static final int REPAIR_PER_STEP = 1;
    private static final int MAX_PER_BREAK = 1;
    private static final int DAMAGED = 100;

    private static RepairSettlement settle(int carry, int hits) {
        return RepairSettlement.settle(carry, hits, HITS_PER_REPAIR, REPAIR_PER_STEP, MAX_PER_BREAK, DAMAGED);
    }

    @Test
    public void discardedHitsDoNotRepair() {
        // 壊さずにやめたブロックのヒットは渡されないので、確定したヒットは0
        RepairSettlement result = settle(3, 0);
        assertEquals(0, result.repair);
        assertEquals(3, result.carry);
    }

    @Test
    public void fewerThanFiveHitsCarryOver() {
        RepairSettlement first = settle(0, 3);
        assertEquals(0, first.repair);
        assertEquals(3, first.carry);
        RepairSettlement second = settle(first.carry, 2);
        assertEquals(1, second.repair);
        assertEquals(0, second.carry);
    }

    @Test
    public void fiveToNineHitsRepairOnce() {
        assertEquals(1, settle(0, 5).repair);
        assertEquals(0, settle(0, 5).carry);
        assertEquals(1, settle(0, 9).repair);
        assertEquals(4, settle(0, 9).carry);
    }

    @Test
    public void hardBlockIsCappedAndKeepsOnlyRemainder() {
        RepairSettlement result = settle(0, 50);
        assertEquals(MAX_PER_BREAK, result.repair);
        assertEquals(0, result.carry);
        RepairSettlement withRemainder = settle(2, 52);
        assertEquals(MAX_PER_BREAK, withRemainder.repair);
        assertEquals(4, withRemainder.carry);
    }

    @Test
    public void higherCapAllowsMoreSteps() {
        assertEquals(10, RepairSettlement.settle(0, 50, 5, 1, 100, DAMAGED).repair);
        assertEquals(6, RepairSettlement.settle(0, 15, 5, 2, 100, DAMAGED).repair);
    }

    @Test
    public void undamagedToolRepairsNothingButCountAdvances() {
        RepairSettlement result = RepairSettlement.settle(4, 3, HITS_PER_REPAIR, REPAIR_PER_STEP, MAX_PER_BREAK, 0);
        assertEquals(0, result.repair);
        assertEquals(2, result.carry);
    }

    @Test
    public void repairDoesNotExceedDamage() {
        assertEquals(2, RepairSettlement.settle(0, 50, 5, 1, 100, 2).repair);
    }

    @Test
    public void zeroCapDisablesRepair() {
        RepairSettlement result = RepairSettlement.settle(0, 50, HITS_PER_REPAIR, REPAIR_PER_STEP, 0, DAMAGED);
        assertEquals(0, result.repair);
        assertEquals(0, result.carry);
    }

    @Test
    public void zeroIntervalDisablesRepair() {
        RepairSettlement result = RepairSettlement.settle(3, 50, 0, REPAIR_PER_STEP, MAX_PER_BREAK, DAMAGED);
        assertEquals(0, result.repair);
        assertEquals(0, result.carry);
    }

    @Test
    public void criticalHitsRepairOnceEveryFive() {
        // 近接のクリティカル（1.3.4）: 1回ずつ精算し、5回ごとに 1 回復する
        int carry = 0;
        int repaired = 0;
        for (int i = 0; i < 12; i++) {
            RepairSettlement result = RepairSettlement.settle(carry, 1, 5, 1, 1, 100);
            carry = result.carry;
            repaired += result.repair;
        }
        assertEquals(2, repaired);
        assertEquals(2, carry);
    }
}

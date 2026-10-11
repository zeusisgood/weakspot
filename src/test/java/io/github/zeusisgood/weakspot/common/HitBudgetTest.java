package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HitBudgetTest {

    private static final int INTERVAL = 6;

    @Test
    public void burstOfFourIsAcceptedAtOnce() {
        HitBudget budget = new HitBudget();
        for (int i = 0; i < HitBudget.BURST; i++) {
            assertTrue("hit " + i, budget.tryUse(100, INTERVAL));
        }
        assertFalse(budget.tryUse(100, INTERVAL));
    }

    @Test
    public void oneHitComesBackPerInterval() {
        HitBudget budget = new HitBudget();
        for (int i = 0; i < HitBudget.BURST; i++) {
            budget.use(100, INTERVAL);
        }
        assertFalse(budget.ok(100 + INTERVAL - 1, INTERVAL));
        assertTrue(budget.tryUse(100 + INTERVAL, INTERVAL));
        assertFalse(budget.ok(100 + INTERVAL, INTERVAL));
        assertTrue(budget.tryUse(100 + 2 * INTERVAL, INTERVAL));
    }

    @Test
    public void steadyHitsAtTheIntervalNeverFail() {
        HitBudget budget = new HitBudget();
        for (long now = 0; now < 1000; now += INTERVAL) {
            assertTrue("tick " + now, budget.tryUse(now, INTERVAL));
        }
    }

    @Test
    public void averageRateIsCapped() {
        // 毎 tick 送っても、受け付けるのは BURST + 経過 / 間隔 回まで
        HitBudget budget = new HitBudget();
        int accepted = 0;
        for (long now = 0; now < 600; now++) {
            if (budget.tryUse(now, INTERVAL)) {
                accepted++;
            }
        }
        assertTrue("accepted " + accepted, accepted <= HitBudget.BURST + 600 / INTERVAL);
    }

    @Test
    public void longIdleDoesNotStoreMoreThanBurst() {
        HitBudget budget = new HitBudget();
        budget.use(0, INTERVAL);
        for (int i = 0; i < HitBudget.BURST; i++) {
            assertTrue(budget.tryUse(100000, INTERVAL));
        }
        assertFalse(budget.ok(100000, INTERVAL));
    }

    @Test
    public void zeroIntervalIsAlwaysOk() {
        HitBudget budget = new HitBudget();
        for (int i = 0; i < 100; i++) {
            assertTrue(budget.tryUse(5, 0));
        }
    }

    @Test
    public void canUseAnEarlierTick() {
        // 掘る前のヒットの予約は、予約した（前の）tick で数える
        HitBudget budget = new HitBudget();
        for (int i = 0; i < HitBudget.BURST; i++) {
            budget.use(110, INTERVAL);
        }
        assertFalse(budget.tryUse(100, INTERVAL));
        assertTrue(budget.tryUse(110 + INTERVAL, INTERVAL));
    }
}

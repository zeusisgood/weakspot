package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ComboFactorTest {

    @Test
    public void factorRisesAtTheSteps() {
        assertEquals(1.0, ComboFactor.factor(0), 0);
        assertEquals(1.0, ComboFactor.factor(24), 0);
        assertEquals(1.25, ComboFactor.factor(25), 0);
        assertEquals(1.5, ComboFactor.factor(50), 0);
        assertEquals(1.5, ComboFactor.factor(99), 0);
        assertEquals(2.0, ComboFactor.factor(100), 0);
        assertEquals(2.5, ComboFactor.factor(250), 0);
        assertEquals(3.0, ComboFactor.factor(500), 0);
        assertEquals(4.0, ComboFactor.factor(1000), 0);
        assertEquals(4.0, ComboFactor.factor(5000), 0);
    }

    @Test
    public void gaugeFillsTowardTheNextStep() {
        assertEquals(25, ComboFactor.nextStep(0));
        assertEquals(0.0, ComboFactor.progressToNext(0), 1e-9);
        assertEquals(0.48, ComboFactor.progressToNext(12), 1e-9);
        assertEquals(0xFFAA00, ComboFactor.nextStepRgb(12));
        // 段階に届いたら、次の段階へ 0 から
        assertEquals(50, ComboFactor.nextStep(25));
        assertEquals(0.0, ComboFactor.progressToNext(25), 1e-9);
        assertEquals(12 / 25.0, ComboFactor.progressToNext(37), 1e-9);
        assertEquals(0xFF5555, ComboFactor.nextStepRgb(37));
        assertEquals(1000, ComboFactor.nextStep(999));
        assertEquals(0xFFD700, ComboFactor.nextStepRgb(999));
        // 最後の段階より先は出さない
        assertEquals(-1, ComboFactor.nextStep(1000));
        assertEquals(-1, ComboFactor.progressToNext(1000), 0);
        assertEquals(-1, ComboFactor.nextStepRgb(5000));
    }
}

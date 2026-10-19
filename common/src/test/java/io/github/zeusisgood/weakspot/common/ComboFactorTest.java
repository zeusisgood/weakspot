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
        assertEquals(2.5, ComboFactor.factor(150), 0);
        assertEquals(3.0, ComboFactor.factor(200), 0);
        assertEquals(3.5, ComboFactor.factor(250), 0);
        assertEquals(4.0, ComboFactor.factor(300), 0);
        assertEquals(4.0, ComboFactor.factor(399), 0);
    }

    @Test
    public void factorKeepsRisingPast300() {
        assertEquals(4.5, ComboFactor.factor(400), 0);
        assertEquals(5.0, ComboFactor.factor(500), 0);
        assertEquals(7.5, ComboFactor.factor(1000), 0);
    }

    @Test
    public void gaugeFillsTowardTheNextStep() {
        assertEquals(25, ComboFactor.nextStep(0));
        assertEquals(0.0, ComboFactor.progressToNext(0), 1e-9);
        assertEquals(0.48, ComboFactor.progressToNext(12), 1e-9);
        // 段階に届いたら、次の段階へ 0 から
        assertEquals(50, ComboFactor.nextStep(25));
        assertEquals(0.0, ComboFactor.progressToNext(25), 1e-9);
        assertEquals(12 / 25.0, ComboFactor.progressToNext(37), 1e-9);
        assertEquals(150, ComboFactor.nextStep(100));
        assertEquals(300, ComboFactor.nextStep(299));
        // 300 から先は 100 ごと（終わりはない）
        assertEquals(400, ComboFactor.nextStep(300));
        assertEquals(0.5, ComboFactor.progressToNext(350), 1e-9);
        assertEquals(1100, ComboFactor.nextStep(1000));
    }
}

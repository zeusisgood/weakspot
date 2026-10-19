package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QueryThrottleTest {

    @Test
    public void asksAgainOnlyAfterTheInterval() {
        QueryThrottle<String> throttle = new QueryThrottle<>(5, false);
        assertTrue(throttle.due("a", 100, false));
        assertFalse(throttle.due("a", 104, false));
        assertTrue(throttle.due("a", 105, false));
    }

    @Test
    public void forceAsksAtOnce() {
        QueryThrottle<String> throttle = new QueryThrottle<>(5, false);
        assertTrue(throttle.due("a", 100, false));
        assertTrue(throttle.due("a", 101, true));
    }

    @Test
    public void aNewTargetIsAskedAtOnceUnlessEachTargetIsKept() {
        QueryThrottle<String> lastOnly = new QueryThrottle<>(5, false);
        assertTrue(lastOnly.due("a", 100, false));
        assertTrue(lastOnly.due("b", 101, false));
        assertTrue(lastOnly.due("a", 102, false));

        QueryThrottle<String> perTarget = new QueryThrottle<>(5, true);
        assertTrue(perTarget.due("a", 100, false));
        assertTrue(perTarget.due("b", 101, false));
        assertFalse(perTarget.due("a", 102, false));
    }

    @Test
    public void resetForgetsEveryTarget() {
        QueryThrottle<String> throttle = new QueryThrottle<>(5, true);
        assertTrue(throttle.due("a", 100, false));
        throttle.reset();
        assertTrue(throttle.due("a", 101, false));
    }
}

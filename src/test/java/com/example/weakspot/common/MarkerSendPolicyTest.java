package com.example.weakspot.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarkerSendPolicyTest {

    @Test
    public void sendsChangesButNotFasterThanTheLimit() {
        MarkerSendPolicy policy = new MarkerSendPolicy(20);
        assertTrue(policy.shouldSend(true, true, 100, 2));
        assertFalse(policy.shouldSend(true, true, 101, 2));
        // 待っている間の変化は、間隔が空いたときに送る
        assertTrue(policy.shouldSend(true, true, 102, 2));
    }

    @Test
    public void doesNotResendUnchangedStateUntilKeepAlive() {
        MarkerSendPolicy policy = new MarkerSendPolicy(20);
        assertTrue(policy.shouldSend(true, true, 0, 2));
        assertFalse(policy.shouldSend(false, true, 10, 2));
        assertFalse(policy.shouldSend(false, true, 19, 2));
        assertTrue(policy.shouldSend(false, true, 20, 2));
    }

    @Test
    public void noKeepAliveWhileHidden() {
        MarkerSendPolicy policy = new MarkerSendPolicy(20);
        assertTrue(policy.shouldSend(true, false, 0, 2));
        assertFalse(policy.shouldSend(false, false, 100, 2));
    }

    @Test
    public void zeroIntervalSendsEveryChange() {
        MarkerSendPolicy policy = new MarkerSendPolicy(20);
        assertTrue(policy.shouldSend(true, true, 5, 0));
        assertTrue(policy.shouldSend(true, true, 5, 0));
    }
}

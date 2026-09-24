package com.example.weakspot.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeakSpotSwitchTest {

    @Test
    public void toggleAlternates() {
        boolean on = true;
        on = WeakSpotSwitch.toggled(on);
        assertFalse(on);
        on = WeakSpotSwitch.toggled(on);
        assertTrue(on);
    }
}

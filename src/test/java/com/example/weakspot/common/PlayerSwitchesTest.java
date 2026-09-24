package com.example.weakspot.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlayerSwitchesTest {

    @Test
    public void unknownPlayerIsEnabled() {
        assertTrue(new PlayerSwitches<String>().isEnabled("a"));
    }

    @Test
    public void toggleIsReflectedPerPlayer() {
        PlayerSwitches<String> switches = new PlayerSwitches<>();
        switches.set("a", false);
        assertFalse(switches.isEnabled("a"));
        assertTrue(switches.isEnabled("b"));
        switches.set("a", true);
        assertTrue(switches.isEnabled("a"));
    }

    @Test
    public void forgetReturnsToEnabled() {
        PlayerSwitches<String> switches = new PlayerSwitches<>();
        switches.set("a", false);
        switches.forget("a");
        assertTrue(switches.isEnabled("a"));
    }
}

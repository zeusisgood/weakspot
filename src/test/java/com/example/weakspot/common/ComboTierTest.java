package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ComboTierTest {

    @Test
    public void tiersByCombo() {
        assertEquals(ComboTier.NONE, ComboTier.of(0));
        assertEquals(ComboTier.NONE, ComboTier.of(1));
        assertEquals(ComboTier.WHITE, ComboTier.of(2));
        assertEquals(ComboTier.WHITE, ComboTier.of(9));
        assertEquals(ComboTier.YELLOW, ComboTier.of(10));
        assertEquals(ComboTier.YELLOW, ComboTier.of(24));
        assertEquals(ComboTier.ORANGE, ComboTier.of(25));
        assertEquals(ComboTier.ORANGE, ComboTier.of(49));
        assertEquals(ComboTier.RED, ComboTier.of(50));
        assertEquals(ComboTier.RED, ComboTier.of(99));
        assertEquals(ComboTier.RAINBOW, ComboTier.of(100));
        assertEquals(ComboTier.RAINBOW, ComboTier.of(1000));
    }
}

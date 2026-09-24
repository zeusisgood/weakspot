package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComboDisplayTest {

    @Test
    public void bouncePeaksOnHitAndSettles() {
        assertEquals(ComboDisplay.BOUNCE_PEAK, ComboDisplay.bounceScale(0), 1e-9);
        assertTrue(ComboDisplay.bounceScale(1) > ComboDisplay.bounceScale(2));
        assertEquals(1.0, ComboDisplay.bounceScale(ComboDisplay.BOUNCE_TICKS), 1e-9);
        assertEquals(1.0, ComboDisplay.bounceScale(100), 1e-9);
    }

    @Test
    public void maxIsShownFromFive() {
        assertFalse(ComboDisplay.showsMax(4));
        assertTrue(ComboDisplay.showsMax(5));
    }

    @Test
    public void shortCombosFadeRightAway() {
        assertEquals(1.0, ComboDisplay.fadeAlpha(3, 0), 1e-9);
        assertEquals(0.5, ComboDisplay.fadeAlpha(3, ComboDisplay.FADE_TICKS / 2.0), 1e-9);
        assertEquals(0.0, ComboDisplay.fadeAlpha(3, ComboDisplay.FADE_TICKS), 1e-9);
    }

    @Test
    public void maxHoldsThenFades() {
        assertEquals(1.0, ComboDisplay.fadeAlpha(23, ComboDisplay.MAX_HOLD_TICKS), 1e-9);
        assertEquals(0.0, ComboDisplay.fadeAlpha(23, ComboDisplay.MAX_HOLD_TICKS + ComboDisplay.FADE_TICKS), 1e-9);
        assertEquals(0.0, ComboDisplay.fadeAlpha(23, 1000), 1e-9);
    }

    @Test
    public void glowFadesOut() {
        assertEquals(1.0, ComboDisplay.glowAlpha(0), 1e-9);
        assertEquals(0.0, ComboDisplay.glowAlpha(ComboDisplay.GLOW_TICKS), 1e-9);
        assertEquals(0.0, ComboDisplay.glowAlpha(-1), 1e-9);
    }
}

package com.example.weakspot.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KindMaskTest {

    @Test
    public void keysRoundTrip() {
        int mask = KindMask.fromKeys(new String[] {"harvest", "BOW", "unknown", " ladder "});
        assertTrue(KindMask.isDisabled(mask, HitKind.HARVEST));
        assertTrue(KindMask.isDisabled(mask, HitKind.BOW));
        assertTrue(KindMask.isDisabled(mask, HitKind.LADDER));
        assertFalse(KindMask.isDisabled(mask, HitKind.MINING));
        assertArrayEquals(new String[] {"bow", "ladder", "harvest"}, KindMask.toKeys(mask));
    }

    @Test
    public void toggles() {
        int mask = KindMask.toggled(0, HitKind.SPRINT);
        assertTrue(KindMask.isDisabled(mask, HitKind.SPRINT));
        assertEquals(0, KindMask.toggled(mask, HitKind.SPRINT));
        assertEquals(0, KindMask.fromKeys(null));
    }
}

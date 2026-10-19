package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModVersionsTest {

    @Test
    public void comparesEachNumber() {
        assertEquals(0, ModVersions.compare("1.5.1", "1.5.1"));
        assertTrue(ModVersions.compare("1.5.0", "1.5.1") < 0);
        assertTrue(ModVersions.compare("1.5.10", "1.5.9") > 0);
        assertTrue(ModVersions.compare("1.4.4", "1.5.0") < 0);
    }

    @Test
    public void missingPartsAreZero() {
        assertEquals(0, ModVersions.compare("1.5", "1.5.0"));
        assertTrue(ModVersions.compare("1.5", "1.5.1") < 0);
    }

    @Test
    public void ignoresNonNumbers() {
        assertEquals(0, ModVersions.compare("1.5.1-beta", "1.5.1"));
        assertEquals(0, ModVersions.compare("x", "0"));
    }

    @Test
    public void keysAreSentWhenTheClientIsNewEnough() {
        assertTrue(ModVersions.hasKeysSince("1.9.5", "1.8.4"));
        assertTrue(ModVersions.hasKeysSince("1.9.6", "1.9.6"));
        assertFalse(ModVersions.hasKeysSince("1.9.5", "1.9.6"));
        assertFalse(ModVersions.hasKeysSince(null, "1.5.1"));
        assertFalse(ModVersions.hasKeysSince("", "1.5.1"));
    }
}

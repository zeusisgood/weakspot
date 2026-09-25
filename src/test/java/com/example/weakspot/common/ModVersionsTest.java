package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
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
}

package com.example.weakspot.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateCheckTest {

    @Test
    public void notifiesNewerVersionUnlessSkipped() {
        assertTrue(UpdateCheck.shouldNotify(true, "1.9.3", ""));
        assertTrue(UpdateCheck.shouldNotify(true, "1.9.3", null));
        assertTrue(UpdateCheck.shouldNotify(true, "1.9.4", "1.9.3"));
        assertFalse(UpdateCheck.shouldNotify(true, "1.9.3", "1.9.3"));
        assertFalse(UpdateCheck.shouldNotify(true, "1.9.3", " 1.9.3 "));
    }

    @Test
    public void doesNotNotifyWithoutNewerVersion() {
        assertFalse(UpdateCheck.shouldNotify(false, "1.9.3", ""));
        assertFalse(UpdateCheck.shouldNotify(true, null, ""));
        assertFalse(UpdateCheck.shouldNotify(true, " ", ""));
    }

    @Test
    public void detectsDifferentMinor() {
        assertTrue(UpdateCheck.differentMinor("1.9.1", "1.10.0"));
        assertTrue(UpdateCheck.differentMinor("1.9.1", "2.0.0"));
        assertTrue(UpdateCheck.differentMinor("1.9.1", "1.10"));
        assertFalse(UpdateCheck.differentMinor("1.9.1", "1.9.2"));
        assertFalse(UpdateCheck.differentMinor("1.9", "1.9.2"));
    }

    @Test
    public void nonNumericVersionsAreNotDifferent() {
        assertFalse(UpdateCheck.differentMinor("1.9.1", "abc"));
        assertFalse(UpdateCheck.differentMinor("dev", "1.10.0"));
        assertFalse(UpdateCheck.differentMinor(null, "1.10.0"));
    }
}

package com.example.weakspot.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarkerColorTest {

    @Test
    public void parsesHexWithOrWithoutHash() {
        assertEquals(0x3FA9FF, MarkerColor.parse("#3FA9FF", 0));
        assertEquals(0x3DDC84, MarkerColor.parse("3ddc84", 0));
        assertEquals(0xB26BFF, MarkerColor.parse("  #B26BFF ", 0));
    }

    @Test
    public void fallsBackOnBadInput() {
        assertEquals(0x123456, MarkerColor.parse("blue", 0x123456));
        assertEquals(0x123456, MarkerColor.parse("#12345", 0x123456));
        assertEquals(0x123456, MarkerColor.parse("#GGGGGG", 0x123456));
        assertEquals(0x123456, MarkerColor.parse(null, 0x123456));
    }

    @Test
    public void towardWhite() {
        assertArrayEquals(new float[] {1, 0.5F, 0.5F}, MarkerColor.towardWhite(0xFF0000, 0.5F), 1e-6F);
        assertArrayEquals(new float[] {1, 1, 1}, MarkerColor.towardWhite(0x000000, 1), 1e-6F);
    }
}

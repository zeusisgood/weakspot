package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HitKindTest {

    @Test
    public void idRoundTrip() {
        for (HitKind kind : HitKind.values()) {
            assertEquals(kind, HitKind.byId(kind.ordinal()));
        }
    }

    @Test
    public void unknownIdIsNull() {
        assertNull(HitKind.byId(-1));
        assertNull(HitKind.byId(HitKind.values().length));
    }
}

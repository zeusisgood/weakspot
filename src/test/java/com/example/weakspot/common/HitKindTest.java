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

    @Test
    public void keyRoundTrip() {
        for (HitKind kind : HitKind.values()) {
            assertEquals(kind, HitKind.byKey(kind.key()));
        }
        assertEquals(HitKind.HARVEST, HitKind.byKey(" Harvest "));
        assertNull(HitKind.byKey("nothing"));
        assertNull(HitKind.byKey(null));
    }

    @Test
    public void newKindsAreAppended() {
        // 通信は番号で送るので、1.6.x までの種類の番号は変えない
        assertEquals(9, HitKind.SLEEP.ordinal());
        assertEquals(10, HitKind.LADDER.ordinal());
        assertEquals(15, HitKind.SPRINT.ordinal());
        assertEquals(16, HitKind.PORTAL.ordinal());
    }
}

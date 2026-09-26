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

    @Test
    public void defaultColorsAndComboFactorKindsMatch188() {
        org.junit.Assert.assertEquals(0xFF5926, HitKind.MINING.defaultColor());
        org.junit.Assert.assertEquals(0xFF5926, HitKind.BOW.defaultColor());
        org.junit.Assert.assertEquals(0xFF3DCB, HitKind.HARVEST.defaultColor());
        org.junit.Assert.assertEquals(0x55CCFF, HitKind.VEHICLE.defaultColor());
        org.junit.Assert.assertEquals(0xFFD23F, HitKind.PORTAL.defaultColor());
        java.util.Set<HitKind> factor = java.util.EnumSet.noneOf(HitKind.class);
        for (HitKind kind : HitKind.values()) {
            if (kind.usesComboFactor()) {
                factor.add(kind);
            }
        }
        org.junit.Assert.assertEquals(java.util.EnumSet.of(HitKind.MINING, HitKind.VEHICLE, HitKind.LADDER,
                HitKind.SPRINT, HitKind.ELYTRA, HitKind.THROW, HitKind.MELEE, HitKind.PORTAL, HitKind.HARVEST), factor);
    }
}

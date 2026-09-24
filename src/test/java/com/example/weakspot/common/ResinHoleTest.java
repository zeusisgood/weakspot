package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ResinHoleTest {

    @Test
    public void dryHolesGiveTheirFacing() {
        assertEquals("north", ResinHole.dryFacing("dry_north"));
        assertEquals("east", ResinHole.dryFacing("dry_east"));
    }

    @Test
    public void otherStatesHaveNoDryHole() {
        assertNull(ResinHole.dryFacing("wet_north"));
        assertNull(ResinHole.dryFacing("plain_y"));
        assertNull(ResinHole.dryFacing("dry_"));
        assertNull(ResinHole.dryFacing(null));
    }
}

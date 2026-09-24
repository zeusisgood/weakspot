package com.example.weakspot.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class MarkerMotionTest {

    @Test
    public void progressStartsAtZeroAndEndsAt80ms() {
        assertEquals(0.0, MarkerMotion.progress(0), 1e-9);
        assertEquals(1.0, MarkerMotion.progress(MarkerMotion.MOVE_MS), 1e-9);
        assertEquals(1.0, MarkerMotion.progress(500), 1e-9);
    }

    @Test
    public void progressIsEaseOut() {
        // 前半で半分より多く進む
        assertTrue(MarkerMotion.progress(MarkerMotion.MOVE_MS / 2) > 0.5);
        assertTrue(MarkerMotion.progress(MarkerMotion.MOVE_MS / 4) > 0.25);
        double previous = 0;
        for (long t = 1; t <= MarkerMotion.MOVE_MS; t++) {
            double p = MarkerMotion.progress(t);
            assertTrue(p >= previous);
            previous = p;
        }
    }

    @Test
    public void positionMovesFromOldToNew() {
        MarkerMotion motion = new MarkerMotion(0, 0);
        motion.moveTo(1, 2, 1000);
        assertArrayEquals(new double[] {0, 0}, motion.position(1000), 1e-9);
        double[] middle = motion.position(1040);
        assertTrue(middle[0] > 0.5 && middle[0] < 1);
        assertEquals(middle[0] * 2, middle[1], 1e-9);
        assertArrayEquals(new double[] {1, 2}, motion.position(1080), 1e-9);
        assertArrayEquals(new double[] {1, 2}, motion.position(5000), 1e-9);
    }

    @Test
    public void interruptedMoveStartsFromThePreviousTarget() {
        MarkerMotion motion = new MarkerMotion(0, 0);
        motion.moveTo(1, 0, 1000);
        motion.position(1020);
        motion.moveTo(1, 1, 1020);
        assertArrayEquals(new double[] {1, 0}, motion.position(1020), 1e-9);
        assertArrayEquals(new double[] {1, 1}, motion.position(1100), 1e-9);
    }

    @Test
    public void interruptedMoveKeepsItsAfterimages() {
        MarkerMotion motion = new MarkerMotion(0, 0);
        motion.moveTo(1, 0, 1000);
        motion.moveTo(1, 1, 1020);
        assertEquals(MarkerMotion.AFTERIMAGE_COUNT * 2, motion.afterimages(1060).size());
    }

    @Test
    public void jumpSwitchesInPlaceWithoutAfterimages() {
        MarkerMotion motion = new MarkerMotion(0, 0);
        motion.jumpTo(1, 1);
        assertArrayEquals(new double[] {1, 1}, motion.position(0), 1e-9);
        assertTrue(motion.afterimages(0).isEmpty());
        assertEquals(0.0, motion.headHighlight(0), 1e-9);
    }

    @Test
    public void sameTargetDoesNotMove() {
        MarkerMotion motion = new MarkerMotion(0.5, 0.5);
        motion.moveTo(0.5, 0.5, 1000);
        assertTrue(motion.afterimages(1000).isEmpty());
        assertEquals(0.0, motion.headHighlight(1000), 1e-9);
    }

    @Test
    public void afterimagesLieOnThePathAndOldestFadesFirst() {
        MarkerMotion motion = new MarkerMotion(0, 0);
        motion.moveTo(1, 0, 1000);
        List<MarkerMotion.Afterimage> all = motion.afterimages(1000 + MarkerMotion.MOVE_MS);
        assertEquals(MarkerMotion.AFTERIMAGE_COUNT, all.size());
        for (int i = 0; i < all.size(); i++) {
            MarkerMotion.Afterimage a = all.get(i);
            assertTrue(a.u >= 0 && a.u < 1);
            assertEquals(0.0, a.v, 1e-9);
            if (i > 0) {
                assertTrue(a.u > all.get(i - 1).u);
                assertTrue(a.bornMs >= all.get(i - 1).bornMs);
            }
        }
        // 最初の残像は、その位置をマーカーが通った瞬間（動き始め）に生まれる
        assertEquals(1000, all.get(0).bornMs);
    }

    @Test
    public void afterimagesAreDroppedOnceFaded() {
        MarkerMotion motion = new MarkerMotion(0, 0);
        motion.moveTo(1, 0, 1000);
        assertTrue(motion.afterimages(1000 + MarkerMotion.MOVE_MS + MarkerMotion.AFTERIMAGE_FADE_MS).isEmpty());
    }

    @Test
    public void afterimageAlphaFadesToZeroIn150ms() {
        assertEquals(1.0, MarkerMotion.afterimageAlpha(0), 1e-9);
        assertEquals(0.5, MarkerMotion.afterimageAlpha(75), 1e-9);
        assertEquals(0.0, MarkerMotion.afterimageAlpha(MarkerMotion.AFTERIMAGE_FADE_MS), 1e-9);
        assertEquals(0.0, MarkerMotion.afterimageAlpha(1000), 1e-9);
        assertEquals(0.0, MarkerMotion.afterimageAlpha(-5), 1e-9);
    }

    @Test
    public void headHighlightOnlyWhileMoving() {
        MarkerMotion motion = new MarkerMotion(0, 0);
        motion.moveTo(1, 0, 1000);
        assertEquals(1.0, motion.headHighlight(1000), 1e-9);
        assertTrue(motion.headHighlight(1040) > 0);
        assertEquals(0.0, motion.headHighlight(1080), 1e-9);
    }

    @Test
    public void switchesInPlaceWithoutPreviousOrOnAnotherSurface() {
        assertTrue(MarkerMotion.animates(true, true, true));
        assertFalse(MarkerMotion.animates(true, false, false));
        assertFalse(MarkerMotion.animates(true, true, false));
        assertFalse(MarkerMotion.animates(false, true, true));
    }

    @Test
    public void timeToReachInvertsProgress() {
        assertEquals(0, MarkerMotion.timeToReach(0));
        long t = MarkerMotion.timeToReach(0.5);
        assertEquals(0.5, MarkerMotion.progress(t), 0.05);
    }
}

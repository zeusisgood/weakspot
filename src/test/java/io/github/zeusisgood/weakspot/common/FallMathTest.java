package io.github.zeusisgood.weakspot.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FallMathTest {

    @Test
    public void noDamageUpToThreeBlocks() {
        assertEquals(0, FallMath.damagePoints(0, 0));
        assertEquals(0, FallMath.damagePoints(3.0, 0));
        assertEquals(1, FallMath.damagePoints(3.2, 0));
        assertEquals(17, FallMath.damagePoints(20, 0));
    }

    @Test
    public void jumpBoostSoftensTheFall() {
        assertEquals(15, FallMath.damagePoints(20, 2));
        assertEquals(0, FallMath.damagePoints(4, 1));
    }

    @Test
    public void resistanceAndProtectionReduceLikeVanilla() {
        assertEquals(17, FallMath.damage(20, 0, 0, 0), 1e-9);
        // 耐性 I で 20% 減
        assertEquals(13.6, FallMath.damage(20, 0, 1, 0), 1e-9);
        // 落下耐性 IV（保護値 12）で 48% 減
        assertEquals(17 * (1 - 12 / 25.0), FallMath.damage(20, 0, 0, 12), 1e-9);
        // 保護値は 20 まで
        assertEquals(17 * 0.2, FallMath.damage(20, 0, 0, 40), 1e-9);
        assertEquals(0, FallMath.damage(20, 0, 5, 0), 1e-9);
    }

    @Test
    public void outlookAgainstHealth() {
        assertEquals(FallMath.Outlook.SAFE, FallMath.outlook(0, 20, 0, false));
        assertEquals(FallMath.Outlook.HURT, FallMath.outlook(19, 20, 0, false));
        assertEquals(FallMath.Outlook.LETHAL, FallMath.outlook(20, 20, 0, false));
        // 衝撃吸収のハートの分は耐える
        assertEquals(FallMath.Outlook.HURT, FallMath.outlook(22, 20, 4, false));
        // トーテムがあれば死なない
        assertEquals(FallMath.Outlook.HURT, FallMath.outlook(40, 20, 0, true));
    }

    @Test
    public void heartsAreHalfThePointsRoundedUp() {
        assertEquals("0", FallMath.heartsLabel(0));
        assertEquals("0.5", FallMath.heartsLabel(1));
        assertEquals("8.5", FallMath.heartsLabel(17));
        assertEquals("10", FallMath.heartsLabel(20));
        assertEquals("4.5", FallMath.heartsLabel(8.84));
    }
}

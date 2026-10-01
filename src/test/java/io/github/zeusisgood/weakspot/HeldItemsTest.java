package io.github.zeusisgood.weakspot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemNameTag;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemRedstone;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;
import org.junit.Test;

/** 右クリックで何もしないアイテムの見分け（1.11.0）。1.12.2 のバニラのクラスで確かめる（SPEC_v1.11.0.md の 7.2）。 */
public class HeldItemsTest {

    @Test
    public void toolsAndPlainItemsDoNothing() {
        assertTrue(HeldItems.doesNothing(Item.class));
        assertTrue(HeldItems.doesNothing(ItemPickaxe.class));
        assertTrue(HeldItems.doesNothing(ItemAxe.class));
        assertTrue(HeldItems.doesNothing(ItemSword.class));
    }

    @Test
    public void itemsWithARightClickActionAreLeftOut() {
        assertFalse(HeldItems.doesNothing(ItemSpade.class));
        assertFalse(HeldItems.doesNothing(ItemHoe.class));
        assertFalse(HeldItems.doesNothing(ItemShears.class));
        assertFalse(HeldItems.doesNothing(ItemDye.class));
        assertFalse(HeldItems.doesNothing(ItemNameTag.class));
        assertFalse(HeldItems.doesNothing(ItemFood.class));
        assertFalse(HeldItems.doesNothing(ItemSeeds.class));
        assertFalse(HeldItems.doesNothing(ItemRedstone.class));
        assertFalse(HeldItems.doesNothing(ItemArmor.class));
        assertFalse(HeldItems.doesNothing(ItemBow.class));
    }
}

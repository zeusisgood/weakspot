package com.example.weakspot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * メインハンドに持っている物に溜める、次の 1 回の強化（両側。1.7.0 の投げる物、1.8.0 の近接）。
 * 溜めは持ち替える（選んでいるスロットが変わる、メインハンドの物の種類が変わる）まで残る。
 * シングルプレイではクライアントとサーバーのプレイヤーが別のオブジェクトなので、同じ表で別々に持てる。
 */
public final class HeldCharge {

    /** 溜めと、溜めたときに持っていたスロットと物。 */
    private static final class Charge {
        double amount;
        int slot;
        Item item;
    }

    private final Predicate<ItemStack> accepts;
    private final Map<EntityPlayer, Charge> charges = Collections.synchronizedMap(new WeakHashMap<>());

    /** accepts は、溜められる物か。 */
    public HeldCharge(Predicate<ItemStack> accepts) {
        this.accepts = accepts;
    }

    /** メインハンドに、溜められる物を持っているか。 */
    public boolean isHolding(EntityPlayer player) {
        ItemStack stack = player.getHeldItemMainhand();
        return !stack.isEmpty() && accepts.test(stack);
    }

    /** ヒットで溜める（溜められる物を持っているときだけ）。max が 0 より大きければ、そこで止める。 */
    public void add(EntityPlayer player, double amount, double max) {
        if (!isHolding(player) || amount <= 0) {
            return;
        }
        Charge charge = current(player);
        if (charge == null) {
            charge = new Charge();
            charge.slot = player.inventory.currentItem;
            charge.item = player.getHeldItemMainhand().getItem();
            charges.put(player, charge);
        }
        charge.amount += amount;
        if (max > 0) {
            charge.amount = Math.min(charge.amount, max);
        }
    }

    /** 今の溜め（持ち替えていたら 0）。 */
    public double amount(EntityPlayer player) {
        Charge charge = current(player);
        return charge == null ? 0 : charge.amount;
    }

    /** 今の溜めを返して、使い切る。 */
    public double take(EntityPlayer player) {
        double amount = amount(player);
        charges.remove(player);
        return amount;
    }

    public void clear(EntityPlayer player) {
        charges.remove(player);
    }

    /** 持ち替えていたら、溜めを消す（毎 tick 呼ぶ）。 */
    public void tick(EntityPlayer player) {
        if (!charges.isEmpty()) {
            current(player);
        }
    }

    /** 持ち替えていなければ、覚えている溜め。持ち替えていたら消して null。 */
    private Charge current(EntityPlayer player) {
        Charge charge = charges.get(player);
        if (charge != null && (charge.slot != player.inventory.currentItem
                || charge.item != player.getHeldItemMainhand().getItem())) {
            charges.remove(player);
            return null;
        }
        return charge;
    }
}

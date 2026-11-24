package io.github.zeusisgood.weakspot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 溜めは持ち替える（選んでいるスロットが変わる、メインハンドの物の種類が変わる）まで残る。
 * シングルプレイではクライアントとサーバーのプレイヤーが別のオブジェクトなので、同じ表で別々に持てる。
 */
public final class HeldCharge {

    private static final class Charge {
        double amount;
        int slot;
        Item item;
    }

    private final Predicate<ItemStack> accepts;
    private final Map<Player, Charge> charges = Collections.synchronizedMap(new WeakHashMap<>());

    /** accepts は、溜められる物か。 */
    public HeldCharge(Predicate<ItemStack> accepts) {
        this.accepts = accepts;
    }

    public boolean isHolding(Player player) {
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && accepts.test(stack);
    }

    /** 溜められる物を持っているときだけ。max が 0 より大きければ、そこで止める。 */
    public void add(Player player, double amount, double max) {
        if (!isHolding(player) || amount <= 0) {
            return;
        }
        Charge charge = current(player);
        if (charge == null) {
            charge = new Charge();
            charge.slot = player.getInventory().selected;
            charge.item = player.getMainHandItem().getItem();
            charges.put(player, charge);
        }
        charge.amount += amount;
        if (max > 0) {
            charge.amount = Math.min(charge.amount, max);
        }
    }

    /** 持ち替えていたら 0。 */
    public double amount(Player player) {
        Charge charge = current(player);
        return charge == null ? 0 : charge.amount;
    }

    public double take(Player player) {
        double amount = amount(player);
        charges.remove(player);
        return amount;
    }

    public void clear(Player player) {
        charges.remove(player);
    }

    /** 毎 tick 呼ぶ。 */
    public void tick(Player player) {
        if (!charges.isEmpty()) {
            current(player);
        }
    }

    /** 持ち替えていたら消して null。 */
    private Charge current(Player player) {
        Charge charge = charges.get(player);
        if (charge != null && (charge.slot != player.getInventory().selected
                || charge.item != player.getMainHandItem().getItem())) {
            charges.remove(player);
            return null;
        }
        return charge;
    }
}

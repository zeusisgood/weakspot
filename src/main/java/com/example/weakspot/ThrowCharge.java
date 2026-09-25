package com.example.weakspot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 投げる物の溜め（両側。1.7.0）。弱点に当てるたびに溜め、次の 1 投の速さを (1 + 溜め) 倍にする。
 * 溜めは持ち替える（選んでいるスロットが変わる、メインハンドの物の種類が変わる）まで残り、投げたら使い切る。
 * サーバーは、投げた瞬間（RightClickItem）に倍率を覚え、同じ tick にワールドに出たその人の投げた物（EntityThrowable）の
 * 速さに掛ける。クライアントはゲージのために同じ溜めを持つ（サーバーの返事を待たずに進める）。
 * シングルプレイではクライアントとサーバーのプレイヤーが別のオブジェクトなので、同じ表で別々に持てる。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ThrowCharge {

    /** 溜めと、溜めたときに持っていたスロットと物。 */
    private static final class Charge {
        double amount;
        int slot;
        Item item;
    }

    /** 投げた瞬間の倍率と tick（サーバー。同じ tick に出た投げた物に掛ける）。 */
    private static final class Thrown {
        final double multiplier;
        final long tick;

        Thrown(double multiplier, long tick) {
            this.multiplier = multiplier;
            this.tick = tick;
        }
    }

    private static final Map<EntityPlayer, Charge> CHARGES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<EntityLivingBase, Thrown> THROWN = Collections.synchronizedMap(new WeakHashMap<>());

    private ThrowCharge() {
    }

    /** 投げる物か（エンダーパール・雪玉・卵・スプラッシュポーション・残留ポーション・エンチャントの瓶と、それを継承した物）。 */
    public static boolean isThrowable(ItemStack stack) {
        Item item = stack.getItem();
        return !stack.isEmpty() && (item instanceof ItemEnderPearl || item instanceof ItemSnowball
                || item instanceof ItemEgg || item instanceof ItemSplashPotion || item instanceof ItemLingeringPotion
                || item instanceof ItemExpBottle);
    }

    /** メインハンドに投げる物を持っているか。 */
    public static boolean isHoldingThrowable(EntityPlayer player) {
        return isThrowable(player.getHeldItemMainhand());
    }

    /** ヒットで溜める（メインハンドに投げる物を持っているときだけ）。 */
    public static void add(EntityPlayer player, double amount) {
        if (!isHoldingThrowable(player) || amount <= 0) {
            return;
        }
        Charge charge = current(player);
        if (charge == null) {
            charge = new Charge();
            charge.slot = player.inventory.currentItem;
            charge.item = player.getHeldItemMainhand().getItem();
            CHARGES.put(player, charge);
        }
        charge.amount += amount;
    }

    /** 今の溜め（持ち替えていたら 0）。 */
    public static double amount(EntityPlayer player) {
        Charge charge = current(player);
        return charge == null ? 0 : charge.amount;
    }

    /** 持ち替えていなければ、覚えている溜め。持ち替えていたら消して null。 */
    private static Charge current(EntityPlayer player) {
        Charge charge = CHARGES.get(player);
        if (charge != null && (charge.slot != player.inventory.currentItem
                || charge.item != player.getHeldItemMainhand().getItem())) {
            CHARGES.remove(player);
            return null;
        }
        return charge;
    }

    public static void clear(EntityPlayer player) {
        CHARGES.remove(player);
    }

    /** 持ち替えたら、溜めを消す（両側。毎 tick）。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !CHARGES.isEmpty()) {
            current(event.player);
        }
    }

    /**
     * 投げる物を右クリックした（投げる直前。クールダウン中なら、ここまで来ない）。溜めを使い切り、サーバーは倍率を覚える。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (event.getHand() != EnumHand.MAIN_HAND || !isThrowable(event.getItemStack())) {
            return;
        }
        double amount = amount(player);
        if (amount <= 0) {
            return;
        }
        CHARGES.remove(player);
        if (!player.world.isRemote) {
            THROWN.put(player, new Thrown(1 + amount, player.world.getTotalWorldTime()));
        }
    }

    /** 同じ tick に出た、その人の投げた物の速さに倍率を掛ける（サーバー。追跡の前なので、見た目の速さも同じになる）。 */
    @SubscribeEvent
    public static void onJoinWorld(EntityJoinWorldEvent event) {
        if (THROWN.isEmpty() || event.getWorld().isRemote || !(event.getEntity() instanceof EntityThrowable)) {
            return;
        }
        EntityThrowable thrown = (EntityThrowable) event.getEntity();
        EntityLivingBase thrower = thrown.getThrower();
        if (thrower == null) {
            return;
        }
        Thrown pending = THROWN.remove(thrower);
        if (pending != null && pending.tick == event.getWorld().getTotalWorldTime()) {
            multiply(thrown, pending.multiplier);
        }
    }

    private static void multiply(Entity entity, double multiplier) {
        entity.motionX *= multiplier;
        entity.motionY *= multiplier;
        entity.motionZ *= multiplier;
    }
}

package io.github.zeusisgood.weakspot;

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
 * 次の 1 投の速さを (1 + 溜め) 倍にする。サーバーは、投げた瞬間（RightClickItem）に倍率を覚え、同じ tick にワールドに出た
 * その人の投げた物（EntityThrowable）の速さに掛ける。クライアントはゲージのために同じ溜めを持つ。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ThrowCharge {

    /** サーバー。同じ tick に出た投げた物に掛ける。 */
    private static final class Thrown {
        final double multiplier;
        final long tick;

        Thrown(double multiplier, long tick) {
            this.multiplier = multiplier;
            this.tick = tick;
        }
    }

    /** 上限なし（ヒットで溜めるときは max に 0 を渡す）。 */
    public static final HeldCharge CHARGES = new HeldCharge(ThrowCharge::isThrowable);
    private static final Map<EntityLivingBase, Thrown> THROWN = Collections.synchronizedMap(new WeakHashMap<>());

    private ThrowCharge() {
    }

    /** それを継承した物も。 */
    public static boolean isThrowable(ItemStack stack) {
        Item item = stack.getItem();
        return !stack.isEmpty() && (item instanceof ItemEnderPearl || item instanceof ItemSnowball
                || item instanceof ItemEgg || item instanceof ItemSplashPotion || item instanceof ItemLingeringPotion
                || item instanceof ItemExpBottle);
    }

    /** 両側で毎 tick。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CHARGES.tick(event.player);
        }
    }

    /** クールダウン中なら、ここまで来ない。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (event.getHand() != EnumHand.MAIN_HAND || !isThrowable(event.getItemStack())) {
            return;
        }
        double amount = CHARGES.take(player);
        if (amount <= 0) {
            return;
        }
        if (!player.world.isRemote) {
            THROWN.put(player, new Thrown(1 + amount, player.world.getTotalWorldTime()));
        }
    }

    /** サーバー。追跡の前なので、見た目の速さも同じになる。 */
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

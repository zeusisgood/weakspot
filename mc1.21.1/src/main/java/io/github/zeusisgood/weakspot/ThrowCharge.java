package io.github.zeusisgood.weakspot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.WindChargeItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 次の 1 投の速さを (1 + 溜め) 倍にする。サーバーは、投げた瞬間（RightClickItem）に倍率を覚え、同じ tick にワールドに出た
 * その人の投げた物の速さに掛ける。クライアントはゲージのために同じ溜めを持つ。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
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

    private static final Map<LivingEntity, Thrown> THROWN = Collections.synchronizedMap(new WeakHashMap<>());

    private ThrowCharge() {
    }

    /** それを継承した物も。ウィンドチャージは雪玉と同じ投げ方なので入れる（1.21.1 版だけ）。 */
    public static boolean isThrowable(ItemStack stack) {
        Item item = stack.getItem();
        return !stack.isEmpty() && (item instanceof EnderpearlItem || item instanceof SnowballItem
                || item instanceof EggItem || item instanceof ThrowablePotionItem
                || item instanceof ExperienceBottleItem || item instanceof WindChargeItem);
    }

    /** 両側で毎 tick。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CHARGES.tick(event.getEntity());
    }

    /** クールダウン中なら、ここまで来ない。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !isThrowable(event.getItemStack())) {
            return;
        }
        double amount = CHARGES.take(player);
        if (amount <= 0) {
            return;
        }
        if (!player.level().isClientSide) {
            THROWN.put(player, new Thrown(1 + amount, player.level().getGameTime()));
        }
    }

    /** サーバー。追跡の前なので、見た目の速さも同じになる。 */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (THROWN.isEmpty() || event.getLevel().isClientSide()
                || !(entity instanceof ThrowableProjectile || entity instanceof AbstractWindCharge)) {
            return;
        }
        if (!(((Projectile) entity).getOwner() instanceof LivingEntity thrower)) {
            return;
        }
        Thrown pending = THROWN.remove(thrower);
        if (pending != null && pending.tick == event.getLevel().getGameTime()) {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(pending.multiplier));
        }
    }
}

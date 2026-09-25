package com.example.weakspot;

import com.example.weakspot.server.MeleeHits;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 近接の溜め（両側。1.8.0）。剣か斧に、近接の弱点に当てるたびに溜め、次に生き物を殴った攻撃をクリティカルにして、
 * ダメージの倍率を 1.5 × (1 + 溜め) にする（CriticalHitEvent）。溜めは持ち替えるまで残り、生き物への攻撃で使い切る
 * （空振り・ブロックでは残る）。クライアントも同じ溜めを持つ（ゲージと、クリティカルの粒子のため）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MeleeCharge {

    /** ジャンプ攻撃のクリティカルと同じ倍率。これに (1 + 溜め) を掛ける。 */
    public static final float CRIT_MULTIPLIER = 1.5F;

    private static final HeldCharge CHARGES = new HeldCharge(MeleeCharge::isWeapon);

    private MeleeCharge() {
    }

    /** 剣か斧（と、それを継承した Mod の武器）か。 */
    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemAxe;
    }

    public static boolean isHoldingWeapon(EntityPlayer player) {
        return CHARGES.isHolding(player);
    }

    /** ヒットで溜める。max が 0 より大きければ、そこで止める。 */
    public static void add(EntityPlayer player, double amount, double max) {
        CHARGES.add(player, amount, max);
    }

    public static double amount(EntityPlayer player) {
        return CHARGES.amount(player);
    }

    public static void clear(EntityPlayer player) {
        CHARGES.clear(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CHARGES.tick(event.player);
        }
    }

    /** 溜めがあれば、この攻撃をクリティカルにして倍率を掛け、溜めを使い切る（両側）。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCriticalHit(CriticalHitEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(event.getTarget() instanceof EntityLivingBase) || CHARGES.amount(player) <= 0) {
            return;
        }
        double charge = CHARGES.take(player);
        event.setResult(Event.Result.ALLOW);
        event.setDamageModifier((float) (CRIT_MULTIPLIER * (1 + charge)));
        if (!player.world.isRemote && player instanceof EntityPlayerMP) {
            MeleeHits.onChargedAttack((EntityPlayerMP) player);
        }
    }
}

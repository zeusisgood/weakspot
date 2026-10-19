package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.server.MeleeHits;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
* 次に生き物を殴った攻撃のダメージの倍率を 1.5 × (1 + 溜め) にする。溜めは持ち替えるまで残り、生き物への攻撃で使い切る
* （空振り・ブロックでは残る）。クライアントも同じ溜めを持つ（ゲージのため）。
*/
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MeleeCharge {

    /** ジャンプ攻撃のクリティカルと同じ倍率。 */
    public static final float CRIT_MULTIPLIER = 1.5F;

    /** ヒットで溜めるときは、上限 meleeChargeMax を渡す。 */
    public static final HeldCharge CHARGES = new HeldCharge(MeleeCharge::isWeapon);

    private MeleeCharge() {
    }

    /** それを継承した Mod の武器も。 */
    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemAxe;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CHARGES.tick(event.player);
        }
    }

    /** 同じ tick に、同じプレイヤーの近接のダメージ（正面の相手と、剣の薙ぎ払いで当たった周り）に倍率を掛ける。 */
    private static final class Charged {
        final float multiplier;
        final long tick;
        final int targetId;

        Charged(float multiplier, long tick, int targetId) {
            this.multiplier = multiplier;
            this.tick = tick;
            this.targetId = targetId;
        }
    }

    private static final Map<UUID, Charged> CHARGED = new HashMap<>();

    /**
    * クリティカルにはしない（バニラの剣の薙ぎ払いは「クリティカルでない」ことが条件なので、溜めた攻撃でも薙ぎ払えるように）。
    * 倍率はサーバーの LivingHurtEvent で掛ける。ジャンプ中などバニラでもクリティカルのときは、クリティカルの倍率を
    * 1.5 × (1 + 溜め) にする（薙ぎ払いはバニラと同じく出ない）。
    */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCriticalHit(CriticalHitEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(event.getTarget() instanceof EntityLivingBase) || CHARGES.amount(player) <= 0) {
            return;
        }
        double charge = CHARGES.take(player);
        float multiplier = (float) (CRIT_MULTIPLIER * (1 + charge));
        boolean critical = event.getResult() == Event.Result.ALLOW
                || (event.getResult() == Event.Result.DEFAULT && event.isVanillaCritical());
        if (critical) {
            event.setDamageModifier(multiplier);
        }
        if (player.world.isRemote || !(player instanceof EntityPlayerMP)) {
            return;
        }
        if (!critical) {
            CHARGED.put(player.getUniqueID(),
                    new Charged(multiplier, player.world.getTotalWorldTime(), event.getTarget().getEntityId()));
        }
        MeleeHits.onChargedAttack((EntityPlayerMP) player);
    }

    /** サーバー。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (CHARGED.isEmpty() || !"player".equals(source.getDamageType())
                || !(source.getTrueSource() instanceof EntityPlayer)
                || source.getImmediateSource() != source.getTrueSource()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        Charged charged = CHARGED.get(player.getUniqueID());
        if (charged == null) {
            return;
        }
        if (charged.tick != player.world.getTotalWorldTime()) {
            CHARGED.remove(player.getUniqueID());
            return;
        }
        event.setAmount(event.getAmount() * charged.multiplier);
        EntityLivingBase target = event.getEntityLiving();
        if (target.getEntityId() == charged.targetId) {
            // クリティカルにはしないが、見た目と音は出す
            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                    player.getSoundCategory(), 1.0F, 1.0F);
            player.onCriticalHit(target);
        }
    }

    /** サーバー。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !CHARGED.isEmpty()) {
            CHARGED.clear();
        }
    }
}

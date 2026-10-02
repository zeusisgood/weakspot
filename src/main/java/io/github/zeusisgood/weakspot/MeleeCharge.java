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
 * 近接の溜め（両側。1.8.0）。剣か斧に、近接の弱点に当てるたびに溜め、次に生き物を殴った攻撃のダメージの倍率を
 * 1.5 × (1 + 溜め) にする（1.11.0 から、クリティカルにはせず LivingHurtEvent で掛ける。剣の薙ぎ払いの周りにも）。溜めは持ち替えるまで残り、生き物への攻撃で使い切る
 * （空振り・ブロックでは残る）。クライアントも同じ溜めを持つ（ゲージのため）。
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

    /**
     * 溜めを使った攻撃（サーバー。1.11.0）: 倍率と、その攻撃の tick・正面の相手。同じ tick に、同じプレイヤーの近接の
     * ダメージ（正面の相手と、剣の薙ぎ払いで当たった周り）に倍率を掛ける。
     */
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
     * 溜めがあれば使い切り、この攻撃のダメージの倍率を 1.5 × (1 + 溜め) にする（両側）。1.11.0 から、クリティカルには
     * しない（バニラの剣の薙ぎ払いは「クリティカルでない」ことが条件なので、溜めた攻撃でも薙ぎ払えるように）。倍率は
     * サーバーの LivingHurtEvent で、正面の相手と薙ぎ払いで当たった周りに掛ける。ジャンプ中などバニラでもクリティカルの
     * ときは、今までどおりクリティカルの倍率を 1.5 × (1 + 溜め) にする（薙ぎ払いはバニラと同じく出ない）。
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

    /** 溜めを使った攻撃のダメージ（正面の相手と薙ぎ払い）に倍率を掛け、正面の相手にはクリティカルの粒と音を出す（サーバー）。 */
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
            // クリティカルにはしないが、見た目と音は今までどおり（バニラのクリティカルと同じ出し方）
            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                    player.getSoundCategory(), 1.0F, 1.0F);
            player.onCriticalHit(target);
        }
    }

    /** 溜めた攻撃の記憶を、その tick の終わりに消す（サーバー）。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !CHARGED.isEmpty()) {
            CHARGED.clear();
        }
    }
}

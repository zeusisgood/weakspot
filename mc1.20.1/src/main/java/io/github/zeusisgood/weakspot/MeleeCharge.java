package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.server.MeleeHits;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    /** それを継承した Mod の武器も（1.20.1 にメイスはない）。 */
    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        CHARGES.tick(event.player);
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
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof LivingEntity) || CHARGES.amount(player) <= 0) {
            return;
        }
        double charge = CHARGES.take(player);
        float multiplier = (float) (CRIT_MULTIPLIER * (1 + charge));
        // Forge がクリティカルにするのと同じ条件（ほかの Mod が ALLOW で強めたクリティカルも、倍率を置き換える）
        boolean critical = event.getResult() == Event.Result.ALLOW
                || (event.isVanillaCritical() && event.getResult() == Event.Result.DEFAULT);
        if (critical) {
            event.setDamageModifier(multiplier);
        }
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!critical) {
            CHARGED.put(player.getUUID(),
                    new Charged(multiplier, player.level().getGameTime(), event.getTarget().getId()));
        }
        MeleeHits.onChargedAttack(serverPlayer);
    }

    /** サーバー。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (CHARGED.isEmpty() || !source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypeTags.IS_PROJECTILE)
                || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != source.getEntity()) {
            return;
        }
        Charged charged = CHARGED.get(player.getUUID());
        if (charged == null) {
            return;
        }
        if (charged.tick != player.level().getGameTime()) {
            CHARGED.remove(player.getUUID());
            return;
        }
        event.setAmount(event.getAmount() * charged.multiplier);
        LivingEntity target = event.getEntity();
        if (target.getId() == charged.targetId) {
            // クリティカルにはしないが、見た目と音は出す
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, player.getSoundSource(), 1.0F, 1.0F);
            player.crit(target);
        }
    }

    /** サーバー。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!CHARGED.isEmpty()) {
            CHARGED.clear();
        }
    }
}

package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.BowMath;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 弓・トライデント・クロスボウ。両側で、それぞれ自分の側のプレイヤーの「使用中のアイテムの残り時間」を減らす（サーバーは威力、
 * クライアントは見た目とゲージ）。ヒットの時点では残り時間を直接変えず、次の tick で減らす（UseTimeCut）。同じ tick のうちに
 * 離したとき（サーバーはパケットをエンティティの tick より先に処理する）は、弓は ArrowLooseEvent の charge に、トライデント・
 * クロスボウは Stop（releaseUsing の直前）で残り時間に足す。
 * 弓を放つときは Stop イベントが ArrowLooseEvent より先に来るので、弓の Stop では消さない（残った分は、次に引き始めたときに消す）。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class BowDraw {

    /** クロスボウの custom_data。装填したときの過剰チャージのヒット数で、撃ったら消す（持ち替えても残すため、アイテムに書く）。 */
    public static final String CROSSBOW_OVERCHARGE = "weakspot_overcharge";

    /** トライデント・花火の persistent data。当たったときのダメージの倍率（威力が飛ぶ速さや baseDamage で決まらないため）。 */
    private static final String DAMAGE_MULTIPLIER = "weakspot_damage_multiplier";

    /** 引き切り（装填し切り）を超えない。 */
    private static final UseTimeCut CUT = new UseTimeCut(BowDraw::isDrawItem,
            (entity, stack, duration, ticks) -> BowMath.addedTicks(stack.getUseDuration(entity) - duration, ticks,
                    fullTicks(entity, stack)));

    /** 引き始めと、矢を放ったときに 0 に戻す。 */
    private static final Map<LivingEntity, Integer> OVERCHARGE = Collections.synchronizedMap(new WeakHashMap<>());

    /** 1 ヒット分を割合で直したときの端数（BowMath.scaledHitTicks）。引き始めに 0 に戻す。 */
    private static final Map<LivingEntity, Integer> CARRY = Collections.synchronizedMap(new WeakHashMap<>());

    /** サーバー。放った tick も覚え、同じ tick に出た矢・トライデント・花火に掛ける。tick の終わりに捨てる。 */
    private static final Map<LivingEntity, Loose> LOOSED = Collections.synchronizedMap(new WeakHashMap<>());

    private static final class Loose {
        final double multiplier;
        final long tick;

        Loose(double multiplier, long tick) {
            this.multiplier = multiplier;
            this.tick = tick;
        }
    }

    private BowDraw() {
    }

    /** Mod の物も、継承していれば対象。 */
    public static boolean isDrawItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof TridentItem || item instanceof CrossbowItem;
    }

    public static boolean isDrawing(LivingEntity entity) {
        if (!entity.isUsingItem()) {
            return false;
        }
        return isDrawItem(entity.getUseItem());
    }

    /** 弓は 20、トライデントは 10、クロスボウは高速装填を含めた装填の時間。 */
    public static int fullTicks(LivingEntity entity, ItemStack stack) {
        if (stack.getItem() instanceof TridentItem) {
            return TridentItem.THROW_THRESHOLD_TIME;
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return CrossbowItem.getChargeDuration(stack, entity);
        }
        return BowMath.FULL_DRAW_TICKS;
    }

    public static int fullTicks(LivingEntity entity) {
        return fullTicks(entity, entity.getUseItem());
    }

    /** まだ反映していない分も含める。 */
    public static int usedTicks(LivingEntity entity) {
        return entity.getTicksUsingItem() + pending(entity);
    }

    public static boolean isFull(LivingEntity entity) {
        return usedTicks(entity) >= fullTicks(entity);
    }

    public static int pending(LivingEntity entity) {
        return CUT.pending(entity);
    }

    /**
     * 1 ヒット分。弓は hitTicks、トライデント・クロスボウは引き切りに対して同じ割合（端数は次のヒットへ持ち越す）。
     * 引き切りを超える分は切り捨てる。実際に進める tick 数を返す。
     */
    public static int add(LivingEntity entity, int hitTicks) {
        int full = fullTicks(entity);
        int carry = CARRY.getOrDefault(entity, 0);
        CARRY.put(entity, BowMath.nextScaledCarry(hitTicks, full, carry));
        int added = BowMath.addedTicks(usedTicks(entity), BowMath.scaledHitTicks(hitTicks, full, carry), full);
        CUT.add(entity, added);
        return added;
    }

    public static int overcharge(LivingEntity entity) {
        Integer hits = OVERCHARGE.get(entity);
        return hits == null ? 0 : hits;
    }

    public static boolean addOvercharge(LivingEntity entity) {
        if (!BowMath.canOvercharge(overcharge(entity))) {
            return false;
        }
        OVERCHARGE.merge(entity, 1, Integer::sum);
        return true;
    }

    /** 装填済みのクロスボウに記録した過剰チャージのヒット数。なければ 0。 */
    public static int crossbowOvercharge(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(CROSSBOW_OVERCHARGE) || !CrossbowItem.isCharged(stack)) {
            return 0;
        }
        return data.copyTag().getInt(CROSSBOW_OVERCHARGE);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        CUT.onUseStart(event);
        OVERCHARGE.remove(event.getEntity());
        CARRY.remove(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        CUT.onUseTick(event);
    }

    /**
     * トライデント・クロスボウを離したとき（releaseUsing の直前。両側）。バニラは残り時間から引きを決めるので、まだ減らして
     * いない分をここで残り時間に入れる。ほかの処理が取り消したあとには動かさない（LOWEST）。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        ItemStack stack = event.getItem();
        if (event.isCanceled() || stack.getItem() instanceof BowItem || !isDrawItem(stack)
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        int full = fullTicks(player, stack);
        int used = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
        Integer ticks = CUT.take(player);
        if (ticks != null) {
            int added = BowMath.addedTicks(used, ticks, full);
            player.useItemRemaining -= added;
            used += added;
        }
        Integer hits = OVERCHARGE.remove(player);
        if (hits == null || hits <= 0 || used < full) {
            return;
        }
        if (stack.getItem() instanceof TridentItem) {
            onTridentRelease(player, stack, BowMath.overchargeMultiplier(hits));
        } else {
            onCrossbowLoad(player, stack, hits);
        }
    }

    /**
     * 激流は、バニラが releaseUsing で押すのと同じ向きに、倍率の残りの分を足す（両側。動きを決めるのは本人のクライアント）。
     * 激流なしは、サーバーが倍率を覚えて、この直後に出るトライデント（onJoinLevel）に付ける。
     */
    private static void onTridentRelease(Player player, ItemStack stack, double multiplier) {
        float strength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        if (!(strength > 0)) {
            if (!player.level().isClientSide) {
                LOOSED.put(player, new Loose(multiplier, player.level().getGameTime()));
            }
            return;
        }
        // バニラの TridentItem#isTooDamagedToUse と同じ
        if (!player.isInWaterOrRain() || stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return;
        }
        Vec3 push = Vec3.directionFromRotation(player.getXRot(), player.getYRot()).scale(strength * (multiplier - 1));
        player.push(push.x, push.y, push.z);
    }

    /** サーバー。バニラが装填できる（矢がある）ときだけ書く。書いた値はクライアントに同期される（ゲージのため）。 */
    private static void onCrossbowLoad(Player player, ItemStack stack, int hits) {
        if (player.level().isClientSide || CrossbowItem.isCharged(stack) || player.getProjectile(stack).isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(CROSSBOW_OVERCHARGE, hits));
    }

    /** 過剰チャージがあれば、サーバーは倍率を覚えて、この直後にワールドに出る矢（onJoinLevel）に掛ける。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        Integer ticks = CUT.take(player);
        if (ticks != null) {
            event.setCharge(event.getCharge() + BowMath.addedTicks(event.getCharge(), ticks));
        }
        Integer hits = OVERCHARGE.remove(player);
        if (hits != null && hits > 0 && !player.level().isClientSide) {
            LOOSED.put(player, new Loose(BowMath.overchargeMultiplier(hits), player.level().getGameTime()));
        }
    }

    /** サーバー。装填済みのクロスボウを撃つ直前（CrossbowItem#use より先）。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player.level().isClientSide || !(stack.getItem() instanceof CrossbowItem)) {
            return;
        }
        int hits = crossbowOvercharge(stack);
        if (hits <= 0) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(CROSSBOW_OVERCHARGE));
        LOOSED.put(player, new Loose(BowMath.overchargeMultiplier(hits), player.level().getGameTime()));
    }

    /**
     * サーバー。矢は力のエンチャントを含むダメージ（矢を作るときにもう入っている）に掛ける。拡散のクロスボウは同じ tick に
     * 3 本出るので、LOOSED は消さずに残す。
     */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (LOOSED.isEmpty() || event.getLevel().isClientSide() || !(event.getEntity() instanceof Projectile projectile)) {
            return;
        }
        if (!(projectile.getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        Loose loose = LOOSED.get(shooter);
        if (loose == null || loose.tick != event.getLevel().getGameTime()) {
            return;
        }
        if (projectile instanceof ThrownTrident || projectile instanceof FireworkRocketEntity) {
            projectile.getPersistentData().putDouble(DAMAGE_MULTIPLIER, loose.multiplier);
        } else if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(arrow.getBaseDamage() * loose.multiplier);
        }
    }

    /** サーバー。 */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if ((direct instanceof ThrownTrident || direct instanceof FireworkRocketEntity)
                && direct.getPersistentData().contains(DAMAGE_MULTIPLIER)) {
            event.setAmount(event.getAmount() * (float) direct.getPersistentData().getDouble(DAMAGE_MULTIPLIER));
        }
    }

    /** サーバー。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!LOOSED.isEmpty()) {
            LOOSED.clear();
        }
    }
}

package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.BowMath;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

/**
 * 両側で、それぞれ自分の側のプレイヤーの「使用中のアイテムの残り時間」を減らす（サーバーは矢の威力、クライアントは見た目とゲージ）。
 * ヒットの時点では残り時間を直接変えず、次の tick で減らす（UseTimeCut）。同じ tick のうちに矢を放ったとき（サーバーはパケットを
 * エンティティの tick より先に処理する）は、ArrowLooseEvent の charge に足す。
 * 放つときは Stop イベントが ArrowLooseEvent より先に来るので、Stop では消さない（残った分は、次に引き始めたときに消す）。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class BowDraw {

    /** 引き切り（20 tick）を超えない。 */
    private static final UseTimeCut CUT = new UseTimeCut(stack -> stack.getItem() instanceof BowItem,
            (entity, stack, duration, ticks) -> BowMath.addedTicks(stack.getUseDuration(entity) - duration, ticks));

    /** 引き始めと、矢を放ったときに 0 に戻す。 */
    private static final Map<LivingEntity, Integer> OVERCHARGE = Collections.synchronizedMap(new WeakHashMap<>());

    /** サーバー。放った tick も覚え、同じ tick に出た矢に掛ける。 */
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

    /** Mod の弓も、BowItem を継承していれば対象。 */
    public static boolean isDrawing(LivingEntity entity) {
        return entity.isUsingItem() && entity.getUseItem().getItem() instanceof BowItem;
    }

    /** まだ反映していない分も含める。 */
    public static int usedTicks(LivingEntity entity) {
        return entity.getTicksUsingItem() + pending(entity);
    }

    public static int pending(LivingEntity entity) {
        return CUT.pending(entity);
    }

    /** 引き切りを超える分は切り捨てる。実際に進める tick 数を返す。 */
    public static int add(LivingEntity entity, int hitTicks) {
        int added = BowMath.addedTicks(usedTicks(entity), hitTicks);
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

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        CUT.onUseStart(event);
        OVERCHARGE.remove(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        CUT.onUseTick(event);
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

    /** サーバー。力のエンチャントを含むダメージ（矢を作るときにもう入っている）に掛ける。 */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (LOOSED.isEmpty() || event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        Loose loose = LOOSED.remove(shooter);
        if (loose != null && loose.tick == event.getLevel().getGameTime()) {
            arrow.setBaseDamage(arrow.getBaseDamage() * loose.multiplier);
        }
    }
}

package com.example.weakspot;

import com.example.weakspot.common.BowMath;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 弓の弱点のヒットで、弓の引きを進める（両側）。サーバーは矢の威力のため、クライアントは弓の見た目（pull）と
 * 引きゲージのために、それぞれ自分の側のプレイヤーの「使用中のアイテムの残り時間」を減らす。
 *
 * ヒットの時点では残り時間を直接変えず、進める tick 数を覚えておき、次の tick で減らす（UseTimeCut。1.8.8）。同じ tick のうちに矢を放ったとき（サーバーはパケットを
 * エンティティの tick より先に処理する）は、ArrowLooseEvent の charge に足す。どちらも引き切り（20 tick）を超えない。
 * 放つときは Stop イベントが ArrowLooseEvent より先に来るので、Stop では消さない（残った分は、次に引き始めたときに消す）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class BowDraw {

    /** まだ反映していない、進める tick 数。引き切り（20 tick）を超えない。 */
    private static final UseTimeCut CUT = new UseTimeCut(stack -> stack.getItem() instanceof ItemBow,
            (stack, duration, ticks) -> BowMath.addedTicks(stack.getMaxItemUseDuration() - duration, ticks));
    /** プレイヤー → 引き切ったあとのヒット（過剰チャージ）の回数。引き始めと、矢を放ったときに 0 に戻す。 */
    private static final Map<EntityLivingBase, Integer> OVERCHARGE = Collections.synchronizedMap(new WeakHashMap<>());
    /** サーバー: 矢を放ったプレイヤー → その矢のダメージに掛ける倍率と、放った tick（同じ tick に出た矢に掛ける）。 */
    private static final Map<EntityLivingBase, Loose> LOOSED = Collections.synchronizedMap(new WeakHashMap<>());

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

    /** 弓を引いている最中か（Mod の弓も、ItemBow を継承していれば対象）。 */
    public static boolean isDrawing(EntityLivingBase entity) {
        return entity.isHandActive() && entity.getActiveItemStack().getItem() instanceof ItemBow;
    }

    /** 引いた時間（tick）。まだ反映していない分も含める。 */
    public static int usedTicks(EntityLivingBase entity) {
        return entity.getItemInUseMaxCount() + pending(entity);
    }

    public static int pending(EntityLivingBase entity) {
        return CUT.pending(entity);
    }

    /** ヒットで進める。引き切りを超える分は切り捨てる。実際に進める tick 数を返す。 */
    public static int add(EntityLivingBase entity, int hitTicks) {
        int added = BowMath.addedTicks(usedTicks(entity), hitTicks);
        CUT.add(entity, added);
        return added;
    }

    public static int overcharge(EntityLivingBase entity) {
        Integer hits = OVERCHARGE.get(entity);
        return hits == null ? 0 : hits;
    }

    /** 引き切ったあとのヒット。上限（BowMath.MAX_OVERCHARGE_HITS）未満なら 1 増やして true。 */
    public static boolean addOvercharge(EntityLivingBase entity) {
        if (!BowMath.canOvercharge(overcharge(entity))) {
            return false;
        }
        OVERCHARGE.merge(entity, 1, Integer::sum);
        return true;
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        CUT.onUseStart(event);
        OVERCHARGE.remove(event.getEntityLiving());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        CUT.onUseTick(event);
    }

    /**
     * 同じ tick のうちに放った矢には、まだ反映していない分を charge に足す。
     * 過剰チャージがあれば、サーバーは倍率を覚えて、この直後にワールドに出る矢（onJoinWorld）に掛ける。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArrowLoose(ArrowLooseEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Integer ticks = CUT.take(player);
        if (ticks != null) {
            event.setCharge(event.getCharge() + BowMath.addedTicks(event.getCharge(), ticks));
        }
        Integer hits = OVERCHARGE.remove(player);
        if (hits != null && hits > 0 && !player.world.isRemote) {
            LOOSED.put(player, new Loose(BowMath.overchargeMultiplier(hits), player.world.getTotalWorldTime()));
        }
    }

    /** 過剰チャージして放った矢のダメージ（パワーのエンチャントを含む）に、倍率を掛ける（サーバー）。 */
    @SubscribeEvent
    public static void onJoinWorld(EntityJoinWorldEvent event) {
        if (LOOSED.isEmpty() || event.getWorld().isRemote || !(event.getEntity() instanceof EntityArrow)) {
            return;
        }
        EntityArrow arrow = (EntityArrow) event.getEntity();
        if (!(arrow.shootingEntity instanceof EntityLivingBase)) {
            return;
        }
        Loose loose = LOOSED.remove(arrow.shootingEntity);
        if (loose != null && loose.tick == event.getWorld().getTotalWorldTime()) {
            arrow.setDamage(arrow.getDamage() * loose.multiplier);
        }
    }
}

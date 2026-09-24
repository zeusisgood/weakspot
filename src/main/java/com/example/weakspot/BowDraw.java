package com.example.weakspot;

import com.example.weakspot.common.BowMath;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 弓の弱点のヒットで、弓の引きを進める（両側）。サーバーは矢の威力のため、クライアントは弓の見た目（pull）と
 * 引きゲージのために、それぞれ自分の側のプレイヤーの「使用中のアイテムの残り時間」を減らす。
 *
 * ヒットの時点では残り時間を直接変えず（非公開のフィールド）、進める tick 数を覚えておき、次の tick の
 * LivingEntityUseItemEvent.Tick（setDuration）で減らす。同じ tick のうちに矢を放ったとき（サーバーはパケットを
 * エンティティの tick より先に処理する）は、ArrowLooseEvent の charge に足す。どちらも引き切り（20 tick）を超えない。
 * 放つときは Stop イベントが ArrowLooseEvent より先に来るので、Stop では消さない（残った分は、次に引き始めたときに消す）。
 * シングルプレイではクライアントとサーバーのプレイヤーが別のオブジェクトなので、同じ表で別々に持てる。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class BowDraw {

    /** プレイヤー → まだ反映していない、進める tick 数。クライアントとサーバーのスレッドの両方から触る。 */
    private static final Map<EntityLivingBase, Integer> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

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
        Integer ticks = PENDING.get(entity);
        return ticks == null ? 0 : ticks;
    }

    /** ヒットで進める。引き切りを超える分は切り捨てる。実際に進める tick 数を返す。 */
    public static int add(EntityLivingBase entity, int hitTicks) {
        int added = BowMath.addedTicks(usedTicks(entity), hitTicks);
        if (added > 0) {
            PENDING.merge(entity, added, Integer::sum);
        }
        return added;
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        PENDING.remove(event.getEntityLiving());
    }

    /** duration は、この tick に 1 減らす前の残り時間。引いた時間は (最大 - duration)。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (PENDING.isEmpty()) {
            // 全部の生き物の、アイテムを使っている間の毎 tick に来るので、ふだんはすぐに返す
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        Integer ticks = PENDING.remove(entity);
        ItemStack stack = event.getItem();
        if (ticks == null || !(stack.getItem() instanceof ItemBow)) {
            return;
        }
        int used = stack.getMaxItemUseDuration() - event.getDuration();
        int added = BowMath.addedTicks(used, ticks);
        if (added > 0) {
            event.setDuration(event.getDuration() - added);
        }
    }

    /** 同じ tick のうちに放った矢には、まだ反映していない分を charge に足す。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArrowLoose(ArrowLooseEvent event) {
        Integer ticks = PENDING.remove(event.getEntityPlayer());
        if (ticks != null) {
            event.setCharge(event.getCharge() + BowMath.addedTicks(event.getCharge(), ticks));
        }
    }
}

package com.example.weakspot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 使っている物（弓・食べ物）の残り時間を、ヒットで縮める共通の部品（両側。1.8.8 で BowDraw と EatDraw から切り出した）。
 * ヒットの時点では残り時間を直接変えず（非公開のフィールド）、縮める tick 数をプレイヤーごとに覚えておき、次の tick の
 * LivingEntityUseItemEvent.Tick（setDuration）で減らす。使い始めたら（Start）捨てる。
 * シングルプレイではクライアントとサーバーのプレイヤーが別のオブジェクトなので、同じ表で別々に持てる。
 * イベントの受け取りは、使う側のクラス（@SubscribeEvent）から onUseStart / onUseTick に渡す。
 */
public final class UseTimeCut {

    /** 縮める量の決め方。duration はこの tick に 1 減らす前の残り時間、ticks は覚えていた量。0 以下なら縮めない。 */
    public interface Limit {
        int cut(ItemStack stack, int duration, int ticks);
    }

    /** プレイヤー → まだ反映していない、縮める tick 数。クライアントとサーバーのスレッドの両方から触る。 */
    private final Map<EntityLivingBase, Integer> pending = Collections.synchronizedMap(new WeakHashMap<>());
    private final Predicate<ItemStack> target;
    private final Limit limit;

    public UseTimeCut(Predicate<ItemStack> target, Limit limit) {
        this.target = target;
        this.limit = limit;
    }

    /** まだ反映していない量。 */
    public int pending(EntityLivingBase entity) {
        Integer ticks = pending.get(entity);
        return ticks == null ? 0 : ticks;
    }

    /** ヒットで縮める量を足す。 */
    public void add(EntityLivingBase entity, int ticks) {
        if (ticks > 0) {
            pending.merge(entity, ticks, Integer::sum);
        }
    }

    /** まだ反映していない量を取り出す（弓は、同じ tick に放った矢の charge に足す）。なければ null。 */
    public Integer take(EntityLivingBase entity) {
        return pending.remove(entity);
    }

    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        pending.remove(event.getEntityLiving());
    }

    public void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (pending.isEmpty()) {
            // 全部の生き物の、アイテムを使っている間の毎 tick に来るので、ふだんはすぐに返す
            return;
        }
        Integer ticks = pending.remove(event.getEntityLiving());
        ItemStack stack = event.getItem();
        if (ticks == null || !target.test(stack)) {
            return;
        }
        int cut = limit.cut(stack, event.getDuration(), ticks);
        if (cut > 0) {
            event.setDuration(event.getDuration() - cut);
        }
    }
}

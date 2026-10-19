package io.github.zeusisgood.weakspot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * ヒットの時点では残り時間（非公開のフィールド）を直接変えず、縮める tick 数をプレイヤーごとに覚えておき、次の tick の
 * LivingEntityUseItemEvent.Tick（setDuration）で減らす。使い始めたら（Start）捨てる。
 * シングルプレイではクライアントとサーバーのプレイヤーが別のオブジェクトなので、同じ表で別々に持てる。
 */
public final class UseTimeCut {

    /** duration はこの tick に 1 減らす前の残り時間、ticks は覚えていた量。0 以下なら縮めない。 */
    public interface Limit {
        int cut(LivingEntity entity, ItemStack stack, int duration, int ticks);
    }

    /** クライアントとサーバーのスレッドの両方から触る。 */
    private final Map<LivingEntity, Integer> pending = Collections.synchronizedMap(new WeakHashMap<>());
    private final Predicate<ItemStack> target;
    private final Limit limit;

    public UseTimeCut(Predicate<ItemStack> target, Limit limit) {
        this.target = target;
        this.limit = limit;
    }

    public int pending(LivingEntity entity) {
        Integer ticks = pending.get(entity);
        return ticks == null ? 0 : ticks;
    }

    public void add(LivingEntity entity, int ticks) {
        if (ticks > 0) {
            pending.merge(entity, ticks, Integer::sum);
        }
    }

    /** 弓は、同じ tick に放った矢の charge に足す。なければ null。 */
    public Integer take(LivingEntity entity) {
        return pending.remove(entity);
    }

    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        pending.remove(event.getEntity());
    }

    public void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (pending.isEmpty()) {
            // 全部の生き物の、アイテムを使っている間の毎 tick に来るので、ふだんはすぐに返す
            return;
        }
        LivingEntity entity = event.getEntity();
        Integer ticks = pending.remove(entity);
        ItemStack stack = event.getItem();
        if (ticks == null || !target.test(stack)) {
            return;
        }
        int cut = limit.cut(entity, stack, event.getDuration(), ticks);
        if (cut > 0) {
            event.setDuration(event.getDuration() - cut);
        }
    }
}

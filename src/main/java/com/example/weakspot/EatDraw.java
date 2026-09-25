package com.example.weakspot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 食事・飲み物の弱点のヒットで、食べ終わる（飲み終わる）までの時間を縮める（両側。1.6.0）。仕組みは弓の引き（BowDraw）と
 * 同じ: ヒットの時点では縮める tick 数を覚えておき、次の tick の LivingEntityUseItemEvent.Tick（setDuration）で減らす。
 * 残りは 1 より小さくしない（その tick の最後にバニラが 1 減らして 0 になり、食べ終わる）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class EatDraw {

    /** プレイヤー → まだ反映していない、縮める tick 数。クライアントとサーバーのスレッドの両方から触る。 */
    private static final Map<EntityLivingBase, Integer> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

    private EatDraw() {
    }

    /** 食べている・飲んでいる最中か。 */
    public static boolean isEating(EntityLivingBase entity) {
        if (!entity.isHandActive()) {
            return false;
        }
        EnumAction action = entity.getActiveItemStack().getItemUseAction();
        return action == EnumAction.EAT || action == EnumAction.DRINK;
    }

    /** ヒットで縮める。 */
    public static void add(EntityLivingBase entity, int ticks) {
        if (ticks > 0) {
            PENDING.merge(entity, ticks, Integer::sum);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        PENDING.remove(event.getEntityLiving());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (PENDING.isEmpty()) {
            return;
        }
        Integer ticks = PENDING.remove(event.getEntityLiving());
        ItemStack stack = event.getItem();
        if (ticks == null) {
            return;
        }
        EnumAction action = stack.getItemUseAction();
        if (action == EnumAction.EAT || action == EnumAction.DRINK) {
            event.setDuration(Math.max(1, event.getDuration() - ticks));
        }
    }
}

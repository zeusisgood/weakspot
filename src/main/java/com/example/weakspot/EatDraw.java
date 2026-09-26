package com.example.weakspot;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 食事・飲み物の弱点のヒットで、食べ終わる（飲み終わる）までの時間を縮める（両側。1.6.0）。仕組みは弓の引き（BowDraw）と
 * 同じ UseTimeCut（1.8.8）。残りは 1 より小さくしない（その tick の最後にバニラが 1 減らして 0 になり、食べ終わる）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class EatDraw {

    private static final UseTimeCut CUT = new UseTimeCut(EatDraw::isFood,
            (stack, duration, ticks) -> duration - Math.max(1, duration - ticks));

    private EatDraw() {
    }

    /** 食べている・飲んでいる最中か。 */
    public static boolean isEating(EntityLivingBase entity) {
        return entity.isHandActive() && isFood(entity.getActiveItemStack());
    }

    private static boolean isFood(ItemStack stack) {
        EnumAction action = stack.getItemUseAction();
        return action == EnumAction.EAT || action == EnumAction.DRINK;
    }

    /** ヒットで縮める。 */
    public static void add(EntityLivingBase entity, int ticks) {
        CUT.add(entity, ticks);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        CUT.onUseStart(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        CUT.onUseTick(event);
    }
}

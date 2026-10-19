package io.github.zeusisgood.weakspot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** 残りは 1 より小さくしない（その tick の最後にバニラが 1 減らして 0 になり、食べ終わる）。 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class EatDraw {

    private static final UseTimeCut CUT = new UseTimeCut(EatDraw::isFood,
            (entity, stack, duration, ticks) -> duration - Math.max(1, duration - ticks));

    private EatDraw() {
    }

    public static boolean isEating(LivingEntity entity) {
        return entity.isUsingItem() && isFood(entity.getUseItem());
    }

    private static boolean isFood(ItemStack stack) {
        UseAnim action = stack.getUseAnimation();
        return action == UseAnim.EAT || action == UseAnim.DRINK;
    }

    public static void add(LivingEntity entity, int ticks) {
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

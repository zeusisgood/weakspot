package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 移動速度の一時的な修正。固定の ID で、かけ直すたびに外して付け直す。保存しない（transient。加速中にワールドを保存・
 * ログアウトしても、速いまま残らない）。
 */
final class SpeedModifier {

    private final ResourceLocation id;

    SpeedModifier(ResourceLocation id) {
        this.id = id;
    }

    /** 1 以下なら外す。 */
    void set(LivingEntity living, double multiplier) {
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(id);
        double extra = TimedBoostMath.extra(multiplier);
        if (extra > 0) {
            speed.addTransientModifier(new AttributeModifier(id, extra, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    void clear(LivingEntity living) {
        set(living, 1);
    }
}

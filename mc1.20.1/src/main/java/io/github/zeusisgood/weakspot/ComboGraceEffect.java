package io.github.zeusisgood.weakspot;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 付いている間、コンボが途切れるまでの時間が延びる。見るのは両側の HitStreak の持ち主（Trinkets.comboResetTicks）。 */
public final class ComboGraceEffect extends MobEffect {

    ComboGraceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD23F);
    }
}

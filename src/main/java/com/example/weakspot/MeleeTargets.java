package com.example.weakspot;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 近接の弱点の対象（両側で同じ条件）。敵（IMob を持つ生き物）だけ。動物・村人・プレイヤーは対象外。
 * 持っているアイテムは問わない（素手を含む）。
 */
public final class MeleeTargets {

    /** バニラのクリティカルと同じ、攻撃のゲージの条件（EntityPlayer#attackTargetEntityWithCurrentItem の 0.9）。 */
    public static final float CHARGED = 0.9F;

    private MeleeTargets() {
    }

    public static boolean isTarget(Entity entity) {
        return entity instanceof IMob && entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)
                && entity.isEntityAlive();
    }

    /**
     * 攻撃のゲージが溜まっているか。adjustTicks は、バニラと同じ 0.5 に、サーバーではネットワークの揺らぎの分を足す。
     */
    public static boolean isCharged(EntityPlayer player, float adjustTicks) {
        return player.getCooledAttackStrength(adjustTicks) > CHARGED;
    }
}

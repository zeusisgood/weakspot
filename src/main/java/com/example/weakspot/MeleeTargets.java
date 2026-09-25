package com.example.weakspot;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 近接の弱点の敵（両側で同じ条件）。IMob を持つ生き物だけ。動物・村人・プレイヤーは対象外。
 * 1.8.0 から、近くに敵がいる間だけ近接の弱点を出す（壁越しでもよい）。
 */
public final class MeleeTargets {

    private MeleeTargets() {
    }

    public static boolean isTarget(Entity entity) {
        return entity instanceof IMob && entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)
                && entity.isEntityAlive();
    }

    /** range ブロック以内に敵がいるか。 */
    public static boolean hasEnemyNear(EntityPlayer player, double range) {
        double rangeSq = range * range;
        for (EntityLivingBase entity : player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                player.getEntityBoundingBox().grow(range), MeleeTargets::isTarget)) {
            if (entity.getDistanceSq(player) <= rangeSq) {
                return true;
            }
        }
        return false;
    }
}

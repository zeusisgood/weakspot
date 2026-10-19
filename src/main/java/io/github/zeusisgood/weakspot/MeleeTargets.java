package io.github.zeusisgood.weakspot;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;

/** IMob を持つ生き物だけ（動物・村人・プレイヤーは対象外）。壁越しでもよい。 */
public final class MeleeTargets {

    private MeleeTargets() {
    }

    public static boolean isTarget(Entity entity) {
        return entity instanceof IMob && entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)
                && entity.isEntityAlive();
    }

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

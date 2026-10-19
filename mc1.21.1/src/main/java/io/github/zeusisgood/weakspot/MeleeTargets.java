package io.github.zeusisgood.weakspot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Enemy を持つ生き物だけ（動物・村人・プレイヤーは対象外）。壁越しでもよい。 */
public final class MeleeTargets {

    private MeleeTargets() {
    }

    public static boolean isTarget(Entity entity) {
        return entity instanceof Enemy && entity instanceof LivingEntity && !(entity instanceof Player)
                && entity.isAlive();
    }

    public static boolean hasEnemyNear(Player player, double range) {
        double rangeSq = range * range;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), MeleeTargets::isTarget)) {
            if (entity.distanceToSqr(player) <= rangeSq) {
                return true;
            }
        }
        return false;
    }
}

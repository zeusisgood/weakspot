package com.example.weakspot.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * 近接の弱点を出す敵を、攻撃が届く距離より遠く（SIGHT_DISTANCE）まで探す（1.3.5）。
 * バニラの照準（EntityRenderer#getMouseOver）と同じ選び方で、視線の先の一番手前の、当たり判定のある生き物を返す。
 * ブロックに遮られていれば返さない。ここで見つけた敵には弱点を出すだけで、当てられるのはバニラの照準の距離だけ。
 */
final class MeleeSight {

    /** 近接の弱点が見える距離（ブロック）。 */
    static final double SIGHT_DISTANCE = 16.0;

    private MeleeSight() {
    }

    /** 視線の先の一番手前の生き物と、視線がその箱に当たる点。なければ null。 */
    static RayTraceResult find(Minecraft mc, float partialTicks) {
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null || mc.world == null) {
            return null;
        }
        Vec3d eye = viewer.getPositionEyes(partialTicks);
        Vec3d look = viewer.getLook(partialTicks);
        Vec3d end = eye.add(look.scale(SIGHT_DISTANCE));
        double limit = SIGHT_DISTANCE;
        RayTraceResult block = mc.world.rayTraceBlocks(eye, end, false, false, true);
        if (block != null && block.typeOfHit == RayTraceResult.Type.BLOCK) {
            limit = eye.distanceTo(block.hitVec);
        }
        AxisAlignedBB area = viewer.getEntityBoundingBox()
                .expand(look.x * SIGHT_DISTANCE, look.y * SIGHT_DISTANCE, look.z * SIGHT_DISTANCE).grow(1.0);
        List<Entity> candidates = mc.world.getEntitiesInAABBexcluding(viewer, area,
                entity -> EntitySelectors.NOT_SPECTATING.apply(entity) && entity != null
                        && entity.canBeCollidedWith());
        Entity closest = null;
        Vec3d closestHit = null;
        double closestDistance = limit;
        for (Entity entity : candidates) {
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
            if (box.contains(eye)) {
                return new RayTraceResult(entity, eye);
            }
            RayTraceResult hit = box.calculateIntercept(eye, end);
            if (hit == null) {
                continue;
            }
            double distance = eye.distanceTo(hit.hitVec);
            if (distance < closestDistance) {
                closest = entity;
                closestHit = hit.hitVec;
                closestDistance = distance;
            }
        }
        return closest == null ? null : new RayTraceResult(closest, closestHit);
    }
}
